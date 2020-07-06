package grakn.simulation;

import grabl.tracing.client.GrablTracing;
import grabl.tracing.client.GrablTracingThreadStatic;
import grakn.client.GraknClient;
import grakn.client.GraknClient.Session;
import grakn.client.GraknClient.Transaction;
import grakn.simulation.agents.World;
import grakn.simulation.agents.base.AgentContext;
import grakn.simulation.agents.base.AgentRunner;
import grakn.simulation.common.RandomSource;
import grakn.simulation.config.Config;
import grakn.simulation.config.ConfigLoader;
import grakn.simulation.yaml_tool.GraknYAMLException;
import grakn.simulation.yaml_tool.GraknYAMLLoader;
import graql.lang.Graql;
import graql.lang.query.GraqlDefine;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static grabl.tracing.client.GrablTracing.tracing;
import static grabl.tracing.client.GrablTracing.tracingNoOp;
import static grabl.tracing.client.GrablTracing.withLogging;

public class Simulation implements AgentContext, AutoCloseable {

    private final static long RANDOM_SEED = 1;
    private final static int DEFAULT_NUM_ITERATIONS = 10;
    private final static int DEFAULT_SCALE_FACTOR = 5;
    private final static String DEFAULT_CONFIG_YAML = "config/config.yaml";
    private final static Logger LOGGER = LoggerFactory.getLogger(Simulation.class);

    private enum SamplingFunction {

        EVERY("every", new int[0]),
        LOG("log", new int[0]);

        private final String name;
        private final int[] acceptedArgs;

        SamplingFunction(String name, int[] acceptedArgs) {
            this.name = name;
            this.acceptedArgs = acceptedArgs;
        }

        public static SamplingFunction validate(Config.TraceSampling func) {
            for (SamplingFunction samplingFunction : SamplingFunction.values()) {
                if (samplingFunction.getName().equals(func.getFunctionName())) {
                    if (samplingFunction.getAcceptedArgs().length == 0
                            || Arrays.stream(samplingFunction.getAcceptedArgs()).anyMatch(func.getArg()::equals)) {
                        return samplingFunction;
                    } else {
                        throw new IllegalArgumentException("Function argument not permitted");
                    }
                }
            }
            throw new IllegalArgumentException("Function name not recognised");
        }

        public String getName() {
            return name;
        }

        public int[] getAcceptedArgs() {
            return acceptedArgs;
        }
    }

    private static Optional<String> getOption(CommandLine commandLine, String option) {
        if (commandLine.hasOption(option)) {
            return Optional.of(commandLine.getOptionValue(option));
        } else {
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        //////////////////////////
        // COMMAND LINE OPTIONS //
        //////////////////////////

        Options options = new Options();
        options.addOption(Option.builder("g")
                .longOpt("grakn-uri").desc("Grakn server URI").hasArg().argName("uri")
                .build());
        options.addOption(Option.builder("t")
                .longOpt("tracing-uri").desc("Grabl tracing server URI").hasArg().argName("uri")
                .build());
        options.addOption(Option.builder("o")
                .longOpt("org").desc("Repository organisation").hasArg().argName("name")
                .build());
        options.addOption(Option.builder("r")
                .longOpt("repo").desc("Grabl tracing repository").hasArg().argName("name")
                .build());
        options.addOption(Option.builder("c")
                .longOpt("commit").desc("Grabl tracing commit").hasArg().argName("sha")
                .build());
        options.addOption(Option.builder("u")
                .longOpt("username").desc("Grabl tracing username").hasArg().argName("username")
                .build());
        options.addOption(Option.builder("a")
                .longOpt("api-token").desc("Grabl tracing API token").hasArg().argName("token")
                .build());
        options.addOption(Option.builder("s")
                .longOpt("seed").desc("Simulation randomization seed").hasArg().argName("seed")
                .build());
        options.addOption(Option.builder("i")
                .longOpt("iterations").desc("Number of simulation iterations").hasArg().argName("iterations")
                .build());
        options.addOption(Option.builder("f")
                .longOpt("scale-factor").desc("Scale factor of iteration data").hasArg().argName("scale-factor")
                .build());
        options.addOption(Option.builder("k")
                .longOpt("keyspace").desc("Grakn keyspace").hasArg().required().argName("keyspace")
                .build());
        options.addOption(Option.builder("b")
                .longOpt("config-file").desc("Configuration file").hasArg().argName("config-file")
                .build());
        options.addOption(Option.builder("d")
                .longOpt("disable-tracing").desc("Disable grabl tracing")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        String graknHostUri = getOption(commandLine, "g").orElse(GraknClient.DEFAULT_URI);
        String grablTracingUri = getOption(commandLine, "t").orElse("localhost:7979");
        String grablTracingOrganisation = commandLine.getOptionValue("o");
        String grablTracingRepository = commandLine.getOptionValue("r");
        String grablTracingCommit = commandLine.getOptionValue("c");
        String grablTracingUsername = commandLine.getOptionValue("u");
        String grablTracingToken = commandLine.getOptionValue("a");

        long seed = getOption(commandLine, "s").map(Long::parseLong).orElseGet(() -> {
            System.out.println("No seed supplied, using random seed: " + RANDOM_SEED);
            return RANDOM_SEED;
        });

        boolean disableTracing = commandLine.hasOption("d");

        int iterations = getOption(commandLine, "i").map(Integer::parseInt).orElse(DEFAULT_NUM_ITERATIONS);
        int scaleFactor = getOption(commandLine, "f").map(Integer::parseInt).orElse(DEFAULT_SCALE_FACTOR);
        String graknKeyspace = commandLine.getOptionValue("k");

        Map<String, Path> files = new HashMap<>();
        for (String filepath : commandLine.getArgList()) {
            Path path = Paths.get(filepath);
            String filename = path.getFileName().toString();
            files.put(filename, path);
        }

        Path configPath = Paths.get(getOption(commandLine, "b").orElse(DEFAULT_CONFIG_YAML));
        Config config = ConfigLoader.loadConfigFromYaml(configPath.toFile());

        ////////////////////
        // INITIALIZATION //
        ////////////////////////////////////

        List<AgentRunner> agentRunners = new ArrayList<>();
        for (Config.Agent agent : config.getAgents()) {
            if (agent.getRun()) {
                AgentRunner<?> runner = AgentRunners.AGENTS.get(agent.getName());
                runner.setTrace(agent.getTrace());
                agentRunners.add(runner);
            }
        }

        Function<Integer, Boolean> iterationSamplingFunc;

        SamplingFunction samplingFunction = SamplingFunction.validate(config.getTraceSampling());

        if(samplingFunction.equals(SamplingFunction.EVERY)){
            iterationSamplingFunc = i -> i % config.getTraceSampling().getArg() == 0;
        } else if (samplingFunction.equals(SamplingFunction.LOG)) {
            iterationSamplingFunc = i -> {
                int base = config.getTraceSampling().getArg();
                return i == (int) Math.log(i) / Math.log(base);
            };
        } else {
            throw new RuntimeException("Function validation failed");
        }

        LOGGER.info("Welcome to the Simulation!");
        LOGGER.info("Parsing world data...");
        World world;
        try {
            world = new World(
                    scaleFactor,
                    files.get("continents.csv"),
                    files.get("countries.csv"),
                    files.get("cities.csv"),
                    files.get("female_forenames.csv"),
                    files.get("male_forenames.csv"),
                    files.get("surnames.csv"),
                    files.get("adjectives.csv"),
                    files.get("nouns.csv")
            );
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        LOGGER.info("Connecting to Grakn...");

        try {
            GraknClient grakn = null;
            GrablTracing tracing = null;
            try {
                if (disableTracing) {
                    tracing = withLogging(tracingNoOp());
                } else if (grablTracingUsername == null) {
                    tracing = withLogging(tracing(grablTracingUri));
                } else {
                    tracing = withLogging(tracing(grablTracingUri, grablTracingUsername, grablTracingToken));
                }
                GrablTracingThreadStatic.setGlobalTracingClient(tracing);
                GrablTracingThreadStatic.openGlobalAnalysis(grablTracingOrganisation, grablTracingRepository, grablTracingCommit);

                grakn = new GraknClient(graknHostUri);

                try (Session session = grakn.session(graknKeyspace)) {
                    // TODO: merge these two schema files once this issue is fixed
                    // https://github.com/graknlabs/grakn/issues/5553
                    try (GrablTracingThreadStatic.ThreadContext context = GrablTracingThreadStatic.contextOnThread("schema", 0)) {
                        loadSchema(session,
                                files.get("schema.gql"),
                                files.get("schema-pt2.gql"));
                    }
                    try (GrablTracingThreadStatic.ThreadContext context = GrablTracingThreadStatic.contextOnThread("data", 0)) {
                        loadData(session,
                                files.get("data.yaml"),
                                files.get("currencies.yaml"),
                                files.get("country_currencies.yaml"),
                                files.get("country_languages.yaml"));
                    }
                }

                try (Simulation simulation = new Simulation(
                        grakn,
                        graknKeyspace,
                        agentRunners,
                        new RandomSource(seed),
                        world
                )) {
                    ///////////////
                    // MAIN LOOP //
                    ///////////////

                    for (int i = 0; i < iterations; ++i) {
                        simulation.iterate(iterationSamplingFunc.apply(i));
                    }
                }
            } finally {
                if (grakn != null) {
                    grakn.close();
                }

                if (tracing != null) {
                    tracing.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        LOGGER.info("Simulation complete");
    }

    private static void loadSchema(Session session, Path... schemaPath) throws IOException {
        System.out.println(">>>> trace: loadSchema: start");
        for (Path path : schemaPath) {
            String schemaQuery = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            System.out.println(">>>> trace: loadSchema - session.tx.write: start");
            try (Transaction tx = session.transaction().write()) {
                System.out.println(">>>> trace: loadSchema - session.tx.write: end");
                tx.execute((GraqlDefine) Graql.parse(schemaQuery));
                tx.commit();
            }
        }
        System.out.println(">>>> trace: loadSchema: end");
    }

    private static void loadData(Session session, Path... dataPath) throws IOException, GraknYAMLException {
        GraknYAMLLoader loader = new GraknYAMLLoader(session);
        for (Path path : dataPath) {
            loader.loadFile(path.toFile());
        }
    }

    private final GraknClient client;
    private final String keyspace;
    private final Session defaultSession;
    private final List<AgentRunner> agentRunners;
    private final Random random;
    private final World world;

    private int simulationStep = 1;

    private final ConcurrentMap<String, Session> sessionMap;

    private Simulation(GraknClient grakn, String keyspace, List<AgentRunner> agentRunners, RandomSource randomSource, World world) {
        client = grakn;
        this.keyspace = keyspace;
        defaultSession = client.session(keyspace);
        this.agentRunners = agentRunners;
        random = randomSource.startNewRandom();
        sessionMap = new ConcurrentHashMap<>();
        this.world = world;
    }

    private void iterate(Boolean traceIteration) {

        LOGGER.info("Simulation step: {}", simulationStep);

        for (AgentRunner agentRunner : agentRunners) {
            agentRunner.iterate(this, RandomSource.nextSource(random), traceIteration);
        }

        closeAllSessionsInMap(); // We want to test opening new sessions each iteration.

        simulationStep++;
    }

    private void closeAllSessionsInMap() {
        for (Session session : sessionMap.values()) {
            session.close();
        }
        sessionMap.clear();
    }
    
    @Override
    public Session getIterationGraknSessionFor(String key) {
        return sessionMap.computeIfAbsent(key, k -> client.session(keyspace)); // Open sessions for new keys
    }

    @Override
    public int getSimulationStep() {
        return simulationStep;
    }

    @Override
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.of(LocalDate.ofYearDay(simulationStep, 1), LocalTime.of(0, 0, 0));
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void close() {
        closeAllSessionsInMap();

        defaultSession.close();

        if (client != null) {
            client.close();
        }
    }
}

package grakn.simulation.grakn;

import graql.lang.Graql;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.statement.Statement;

import java.util.List;

import static grakn.simulation.grakn.ExecutorUtils.getOrderedAttribute;

public class MarriageAgent extends grakn.simulation.agents.interaction.MarriageAgent {

    @Override
    protected List<String> getSingleWomen() {
        GraqlGet.Unfiltered singleWomenQuery = getSinglePeopleOfGenderQuery("female", "marriage_wife");
        log().query("getSingleWomen", singleWomenQuery);
        return getOrderedAttribute(tx().forGrakn(), singleWomenQuery, "email");
    }

    @Override
    protected List<String> getSingleMen() {
        GraqlGet.Unfiltered singleMenQuery = getSinglePeopleOfGenderQuery("male", "marriage_husband");
        log().query("getSingleMen", singleMenQuery);
        return getOrderedAttribute(tx().forGrakn(), singleMenQuery, "email");
    }

    private GraqlGet.Unfiltered getSinglePeopleOfGenderQuery(String gender, String marriageRole) {
        Statement personVar = Graql.var("p");
        Statement cityVar = Graql.var("city");

        return Graql.match(
                personVar.isa("person").has("gender", gender).has("email", Graql.var("email")).has("date-of-birth", Graql.var("dob")),
                Graql.var("dob").lte(dobOfAdults()),
                Graql.not(Graql.var("m").isa("marriage").rel(marriageRole, personVar)),
                Graql.var("r").isa("residency").rel("residency_resident", personVar).rel("residency_location", cityVar),
                Graql.not(Graql.var("r").has("end-date", Graql.var("ed"))),
                cityVar.isa("city").has("location-name", city().name())
        ).get("email");
    }

    @Override
    protected void insertMarriage(String wifeEmail, String husbandEmail) {
        int marriageIdentifier = (wifeEmail + husbandEmail).hashCode();

        GraqlInsert marriageQuery = Graql.match(
                Graql.var("husband").isa("person").has("email", husbandEmail),
                Graql.var("wife").isa("person").has("email", wifeEmail),
                Graql.var("city").isa("city").has("location-name", city().name())
        ).insert(
                Graql.var("m").isa("marriage")
                        .rel("marriage_husband", "husband")
                        .rel("marriage_wife", "wife")
                        .has("marriage-id", marriageIdentifier),
                Graql.var().isa("locates").rel("locates_located", Graql.var("m")).rel("locates_location", Graql.var("city"))
        );
        log().query("insertMarriage", marriageQuery);
        tx().forGrakn().execute(marriageQuery);
    }
}

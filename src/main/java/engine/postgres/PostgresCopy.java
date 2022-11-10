package engine.postgres;

//copy test from '/users/alexandr/IdeaProjects/mets/Data/csv/NewData.csv' DELIMITERS E'\t' CSV HEADER;

public class PostgresCopy {

public void test() {


    // Get the underlying PGConnection:
    //PGConnection pgConnection = PostgreSqlUtils.getPGConnection(connection);

    String schemaName = "public";
    String tableName = "test";
    String[] columnNames = new String[] {
            "fm",
            "im",
            "ot"
    };
   // Create the Table Definition:
    //SimpleRowWriter.Table table = new SimpleRowWriter.Table(schemaName, tableName, columnNames);
}
}

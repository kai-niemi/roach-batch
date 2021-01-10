package io.roach.batch.flatfile.schema;

public class TableSchema {
    private String tableName;

    private String create;

    private String insert;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getCreate() {
        return create;
    }

    public void setCreate(String create) {
        this.create = create;
    }

    public String getInsert() {
        return insert;
    }

    public void setInsert(String insert) {
        this.insert = insert;
    }

    @Override
    public String toString() {
        return "TableSchema{" +
                "tableName='" + tableName + '\'' +
                ", create='" + create + '\'' +
                ", insert='" + insert + '\'' +
                '}';
    }
}

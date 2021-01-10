# Roach Batch 

A batch-oriented data conversion and importing tool for CockroachDB, based on [Spring Batch](https://spring.io/projects/spring-batch#overview). 
It can be used to read raw import files of various formats and convert these to a format compatible with CockroachDB
IMPORT and IMPORT INTO commands. Alternatively, import directly to a target database using JDBC batch inserts.  

Fixed width and delimited flat-files are the default:

- A) Local or cloud storage flat file -> CSV import file/stream
- B) Local or cloud storage flat file -> SQL batch inserts

The default method A) is to grab a fixed-width flat file either from the local file system
or a cloud storage bucket (S3), convert it to CSV format and pipe it to an output stream of choice. 
A JSON document is used to define the source file layout (schema).

The B) method does the same but uses JDBC batch inserts to directly write the converted items 
to a target table.

Other potential mappings include:

- SQL query -> CSV import file/stream
- SQL query -> SQL batch inserts
- Kafka Topic -> CSV import
- Kafka Topic -> SQL batch inserts
- .. and so forth

All available batch readers and writers:  

- [Item Readers](https://docs.spring.io/spring-batch/docs/4.3.x/reference/html/appendix.html#listOfReadersAndWriters)
- [Item Writers](https://docs.spring.io/spring-batch/docs/4.3.x/reference/html/appendix.html#itemWritersAppendix)

## Building

See _Project Setup_ section below for building the single binary from source.

## Usage

Roach Batch provides both a built-in shell (the default) and an HTTP API listener. The latter 
is suitable to use in a proxy mode with CockroachDB IMPORT and IMPORT INTO commands, or wget / curl.

Start the tool with:

    ./roach-batch.jar

Type `help` for additional guidance.

To start in online mode with the HTTP API/proxy:

    ./target/roach-batch.jar --proxy
                                  
Main endpoint:

    http://localhost:8080/download

### Configuration

All parameters in `application.yaml` can be overridden via CLI. See
[Common Application Properties](http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
for details.

### Deployment

Roach Batch is a self-contained Spring Boot that is easy to deploy as a service. Here's an
[example](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#deployment-service)
of starting a Unix/Linux service via `init.d` or `system.d`.

## Flat File Example

This example reads a local or cloud storage fixed width flat file, map the contents to a 
delimited text format and import to CockroachDB using IMPORT or IMPORT INTO.

### Source File

Assume a fixed width flat file containing orders:

    <REM> A comment
    -- A comment
    # A comment
    UK21341EAH4121131.11customer1
    UK21341EAH4221232.11customer2
    UK21341EAH4321333.11customer3
    UK21341EAH4421434.11customer4

### Source Schema Example

_See appendix for schema details._

Sample schema for the source file:

    {
        "name": "orders",
        "comments": [
            "--",
            "<REM>",
            "#"
        ],
        "fields": [
            {
                "name": "SKU",
                "range": {
                    "min": 1,
                    "max": 12
                }
            },
            {
                "name": "Quantity",
                "range": {
                    "min": 13,
                    "max": 15
                }
            },
            {
                "name": "Price",
                "range": {
                    "min": 16,
                    "max": 20
                }
            },
            {
                "name": "Total",
                "expression": "#fieldSet.readLong(\"Price\") * #fieldSet.readLong(\"Quantity\")"
            },
            {
                "name": "Customer",
                "range": {
                    "min": 21,
                    "max": 29
                }
            }
        ],
        "tokenizer": {
            "type": "fixed",
            "strict": true
        }
    }
       
### Destination Example

Resulting output when processing the source file, ready to be consumed by IMPORT:

    SKU,Quantity,Price,Total,Customer
    UK21341EAH41,211,31.11,6541,customer1
    UK21341EAH42,212,32.11,6784,customer2
    UK21341EAH43,213,33.11,7029,customer3
    UK21341EAH44,214,34.11,7276,customer4

### Commands

In CLI/shell mode:

    offline:$ flat-to-csv --input-file s3://bucket-name/test.txt --schema-file s3://bucket-name/test.json

In HTTP proxy mode using curl:

    curl --location --request GET 'http://localhost:8080/download?source=s3://bucket-name/test.txt?AWS_ACCESS_KEY_ID=<key>&AWS_SECRET_ACCESS_KEY=<key>&AWS_DEFAULT_REGION=eu-central-1&schema=s3://bucket-name/test.json'

In HTTP proxy mode using SQL:

    create table orders
    (
        sku      string      not null,
        qty      string      not null,
        price    string      not null,
        total    string      not null,
        customer string      not null
    );
    
    import into orders(sku,qty,price,total,customer)
        CSV DATA (
            'http://localhost:8080/download?source=s3://bucket-name/test.txt?AWS_ACCESS_KEY_ID=<key>&AWS_SECRET_ACCESS_KEY=<key>&AWS_DEFAULT_REGION=eu-central-1&schema=s3://bucket-name/test.json'
    );

### S3 Authentication

TBA

---

## Project Setup

### Prerequisites

- JDK8+ with 1.8 language level (OpenJDK compatible)
- Maven 3+ (optional, embedded)

Install the JDK (Linux):

    sudo apt-get -qq install -y openjdk-8-jdk

### Clone the project

    git clone git@github.com:cockroachlabs/roach-batch.git
    cd roach-batch

### Build the executable jar 

    chmod +x mvnw
    ./mvnw clean install

---

## Appendix: Flat File Layout Schema

The purpose of the field layout schema (in JSON format) is to describe the layout
of the source files. Each fixed range field is mapped to a column in the delimited
destination output.

The schema is fairly self-explanatory with the following key attributes:

* name - used for logging
* comments - collection of comment prefixes
* fields - collection of fields to map
    * name - column name
    * range - field position with start and end range
    * expression - Defines a formatting expression based on [SpEL](https://docs.spring.io/spring-framework/docs/4.3.12.RELEASE/spring-framework-reference/html/expressions.html).
      The following object references are pre-set:
        * _fieldSet_
          represents the [fieldset](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/file/transform/FieldSet.html) of the current line
        * The _names_
          represents an array of the field names
        * The _values_
          represents an array of the current line field values
* tokenizer - attributes for the tokenizer
    * type - fixed|delimited|regex
    * strict - less lenient to errors
    * pattern - for regex mode, a regular expression to match lines to import
    * quoteCharacter - for delimited mode, default is '"'
    * delimiter - for delimited mode, default is ";"
* tableSchema - metadata used for SQL batch inserts
    * create - the DDL statement for creating a table
    * insert - the DML statement for importing, column names and number should match fields

-- eof
# spark-jdbc-sample-splitter
spark-jdbc-sample-splitter is spark's DataSourceV2 JDBC partition reader. It makes partition query automatically when read RDB Table or Query.
## Purpose
Spark supports JDBC datasource with partition the table when reading in parallel. But the user must set some tired options like `partitionColumn`, `lowerBound` and `upperBound`. These options must be set when read JDBC in parallel. It doesn't matter if you use it sometimes. However, if you use often, this process -check table and set options- will be very tiring.
## Idea
Spark's JDBC partition table strategy work well when `partitionColumn` is `LongType` and also `partitionColumn`'s value is evenly ditributed. However, some table doesn't have this pretty column to partition. So my idea is that before read table, check table's distribution and make partition query via `Sampling`.

[How spark jdbc parititon option work](https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html#data-source-option) (check partitionColumn, lowerBound, upperBound' Meaning section)
## How to


## To do
- [ ] test using RDB (postgres)
- [ ] optimizing get the sample number algorithm (please help me)
- [ ] optimize for sharded DB
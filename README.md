# spark-jdbc-sample-splitter
spark-jdbc-sample-splitter is spark's DataSourceV2 JDBC partition reader. It makes partition query automatically when read RDB Table or Query.
## Purpose
Spark supports JDBC datasource with partition the table when reading in parallel. But the user must set some tired options like `partitionColumn`, `lowerBound` and `upperBound`. These options must be set when read JDBC in parallel. It doesn't matter if you use it sometimes. However, if you use often, this process -check table and set options- will be very tiring. I want to put this boring work on the machine.
## Idea
Spark's JDBC partition table strategy work well when `partitionColumn` is `LongType` and also `partitionColumn`'s value is evenly ditributed. However, some table doesn't have this pretty column to partition. To partition these tables evenly, you need to find out the distribution of the tables and divide the partitions based on the distribution. However, it is very inefficient to read the entire table to divide the table. So my idea is that using sampling to find out the distribution of tables. Create partitions through the distribution of samples and apply them to the entire table.

[How spark jdbc parititon option work](https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html#data-source-option) (check partitionColumn, lowerBound, upperBound' Meaning section)
## How to
1. Count total number of rows in the target table.
2. Cacluate proper number of samples based on the row count.
3. Execute sampling query to get all samples.
4. Sort samples based on `partitionColumn`
5. Make sample partition through `numPartitions`
6. Apply sample partition to the entire table
## Test
[Test with python](https://github.com/an004005/spark-jdbc-sample-splitter/blob/main/test/sampling%20split%20test.ipynb)
### Sample Size
The above test shows that the larger the sample size, the closer to ideal partition size. However, we should have to find proper sample size because the larger the sample size, the longer the sampling execution time. So we need to calculate the sample size from total number of data and `numPartitions`. But Im not good at proof of these things. **Please someone help me to calcuate to get proper sample size.**
## Why should have to partition evenly


## To do
- [ ] test using RDB (postgres)
- [ ] optimizing get the sample number algorithm (please help me)
- [ ] optimize for sharded or partitioned DB

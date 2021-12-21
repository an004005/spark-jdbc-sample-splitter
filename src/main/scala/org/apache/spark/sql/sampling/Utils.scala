package org.apache.spark.sql.sampling

object Utils {
  def tryResources[A <: AutoCloseable, T](autoCloseable: A)(exec: A => T): T = {
    try {
      exec.apply(autoCloseable)
    } finally {
      if (autoCloseable != null) {
        autoCloseable.close()
      }
    }
  }

  def getSamplingCount(totalCount: Long): Long = {
    totalCount / 100
  }

  def getSamplePercent(totalCount: Long): Double =
    (getSamplingCount(totalCount).toDouble / totalCount) * 100
}

package org.apache.spark.sql.sampling

object Utils {
  object ResourceManager {
    private class AutoCloseManager {
      private[this] var autoCloseables = List[AutoCloseable]()

      def apply[T <: AutoCloseable](autoCloseable: T): T = {
        autoCloseables = autoCloseable :: autoCloseables
        autoCloseable
      }

      def closeAll(): Unit = {
        autoCloseables.flatMap(Option(_)).foreach(_.close())
      }
    }

    def apply(run: AutoCloseManager => Unit): Unit = {
      val manager = new AutoCloseManager
      try {
        run(manager)
      } finally {
        manager.closeAll()
      }
    }

    def apply[R](run: AutoCloseManager => R): R = {
      val manager = new AutoCloseManager
      try {
        run(manager)
      } finally {
        manager.closeAll()
      }
    }
  }

  def getSamplingCount(totalCount: Long): Long = {
    if (totalCount < 1000) {
      totalCount
    } else {
      totalCount / 100
    }
  }

  def getSamplePercent(totalCount: Long): Double = {
    BigDecimal((getSamplingCount(totalCount).toDouble / totalCount) * 100)
      .setScale(2, BigDecimal.RoundingMode.HALF_UP)
      .toDouble
  }
}

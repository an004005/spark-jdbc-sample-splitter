package org.apache.spark.sql.jdbc.sampling

object Utils {
  object ResourceManager {
    private class AutoCloseManager {
      private[this] var autoCloseableList = List[AutoCloseable]()

      def apply[T <: AutoCloseable](autoCloseable: T): T = {
        autoCloseableList = autoCloseable :: autoCloseableList
        autoCloseable
      }

      def closeAll(): Unit = {
        // close reverse order of applied
        autoCloseableList.flatMap(Option(_)).foreach(_.close())
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
}

package com.naoido.sql

import com.naoido.sql.enum.Table
import java.sql.DriverManager
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

class SqlCore {
    companion object {
        private var user: String;
        private var password: String;
        private const val SQL_URL: String = "jdbc:mysql://192.168.100.50:3306/user_history";
        val INSTANCE: SqlCore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SqlCore() };

        init {
            Class.forName("com.mysql.cj.jdbc.Driver");

            val properties: Properties = Properties();
            properties.load(this::class.java.getResourceAsStream("/sql.properties"));
            this.user = properties.getProperty("sql-user");
            this.password = properties.getProperty("sql-password");
        }

        fun values(table: Table, value: String) {
            DriverManager.getConnection(SQL_URL, user, password).use { connection ->
                val query: String = "INSERT INTO $table ${table.info} VALUES $value";
                connection.createStatement().executeUpdate(query);
            }
        }

        fun where(table: Table, select: HashMap<String, Any>?, vararg search: Pair<String, Any>): ResultSet {
            DriverManager.getConnection(SQL_URL, user, password).let {connection ->
                val selector = select?.let { createQueryByMap(select) } ?: "*";
                val query: String = "SELECT $selector FROM $table WHERE ${createSearchQuery(*search)}";
                return connection.createStatement().executeQuery(query);
            }
        }

        fun update(table: Table, updateData: List<Pair<String, Any>>, vararg search: Pair<String, Any>) {
            DriverManager.getConnection(SQL_URL, user, password).use { connection ->
                val query: String = "UPDATE $table SET ${createSetQuery(updateData)} WHERE ${createSearchQuery(*search)}"
                connection.createStatement().executeUpdate(query);
            }
        }

        private fun createQueryByMap(selector: HashMap<String, Any>): String {
            var line = "";
            for (select in selector) {
                line += select.key + "=" + (if (select.value is String) "'${select.value}'" else select.value);
                line += ","
            }
            return line.substring(0, line.length - 1);
        }

        private fun createSearchQuery(vararg searchQuery: Pair<String, Any>): String {
            var line = "";
            line += "${searchQuery.first().first}=${searchQuery.first().second}"
            if (searchQuery.size > 1) {
                for (query in searchQuery.copyOfRange(1, searchQuery.size)) {
                    line += when (query.second) {
                        is String -> " AND ${query.first}='${query.second}'";
                        else -> " AND ${query.first}=${query.second}";
                    }
                }
            }
            return line;
        }

        private fun createSetQuery(setQuery: List<Pair<String, Any>>): String {
            var line = "";
            for (query in setQuery) {
                line += "${query.first}=${when(query.second) {is String -> "'${query.second}'" else -> query.second}}";
                line += ","
            }
            return line.substring(0, line.length - 1);
        }

        fun getTimestampNow(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(System.currentTimeMillis()));
        }
    }
}

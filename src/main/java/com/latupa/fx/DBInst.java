package com.latupa.stock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBInst {
        private String url;
        private String username;
        private String password;
        
        private Connection conn = null;
        private PreparedStatement statement = null;
        
        public DBInst(String url, String username, String password) {
                this.url        = url;
                this.username        = username;
                this.password        = password;
        }

        // connect to MySQL
        void connSQL() {
                try { 
                        Class.forName("com.mysql.jdbc.Driver"); 
                        conn = DriverManager.getConnection(url, username, password); 
                }
                //捕获加载驱动程序异常
                 catch ( ClassNotFoundException cnfex ) {
                         System.err.println(
                         "装载 JDBC/ODBC 驱动程序失败。" );
                         cnfex.printStackTrace(); 
                         System.exit(1);
                 } 
                 //捕获连接数据库异常
                 catch ( SQLException sqlex ) {
                         System.err.println( "无法连接数据库" );
                         sqlex.printStackTrace(); 
                         System.exit(1);
                 }
                
                updateSQL("set names utf8");
        }

        // disconnect to MySQL
        void deconnSQL() {
                try {
                        if (conn != null)
                                conn.close();
                } catch (Exception e) {
                        System.out.println("关闭数据库问题 ：");
                        e.printStackTrace();
                }
        }
        
        void closeSQL(ResultSet rs) {
        	try {
				rs.close();
				statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        // execute selection language
        ResultSet selectSQL(String sql) {
                
                if (conn == null) {
                        connSQL();
                }
                
                ResultSet rs = null;
                try {
                        statement = conn.prepareStatement(sql);
                        rs = statement.executeQuery(sql);
                } catch (SQLException e) {
                        System.out.println("查询数据库时出错：" + sql);
                        //e.printStackTrace();
                }
                return rs;
        }

        //execute update language
        boolean updateSQL(String sql) {
                
                if (conn == null) {
                        connSQL();
                }
                
                try {
                        statement = conn.prepareStatement(sql);
                        statement.executeUpdate();
                        statement.close();
                        return true;
                } catch (SQLException e) {
                        System.out.println("更新数据库时出错：" + sql);
                        e.printStackTrace();
                } catch (Exception e) {
                        System.out.println("更新时出错：" + sql);
                        e.printStackTrace();
                }
                return false;
        }
        
}

/*
 * <<
 *  EDP
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package top.datawork.core.utils;

import com.alibaba.druid.util.StringUtils;
import top.datawork.core.common.jdbc.ExtendedJdbcClassLoader;
import top.datawork.core.common.jdbc.JdbcDataSource;
import top.datawork.core.consts.Consts;
import top.datawork.core.enums.DataTypeEnum;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.SourceException;
import top.datawork.core.model.CustomDataSource;
import top.datawork.core.model.JdbcSourceInfo;
import top.datawork.fastbi.runner.LoadSupportDataSourceRunner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

@Slf4j
public class SourceUtils {

    private JdbcDataSource jdbcDataSource;

    @Getter
    private static Set releaseSourceSet = new HashSet();

    public SourceUtils(JdbcDataSource jdbcDataSource) {
        this.jdbcDataSource = jdbcDataSource;
    }

    /**
     * 测试数据源
     *
     * @param jdbcSourceInfo
     * @return
     * @throws SourceException
     */
    public boolean testSource(JdbcSourceInfo jdbcSourceInfo) {

        try {
            Class.forName(getDriverClassName(jdbcSourceInfo.getJdbcUrl(), jdbcSourceInfo.getDbVersion()));
        } catch (ClassNotFoundException e) {
            log.error(e.toString(), e);
            return false;
        }

        try (Connection con = DriverManager.getConnection(jdbcSourceInfo.getJdbcUrl(), jdbcSourceInfo.getUsername(), jdbcSourceInfo.getPassword());) {
            return con != null;
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }

        return false;
    }

    /**
     * 获取数据源
     *
     * @param jdbcSourceInfo
     * @return
     * @throws SourceException
     */
	public DataSource getDataSource(JdbcSourceInfo jdbcSourceInfo) throws SourceException {
		return jdbcDataSource.getDataSource(jdbcSourceInfo);
	}

    public Connection getConnection(JdbcSourceInfo jdbcSourceInfo) throws SourceException {
        Connection conn = getConnectionWithRetry(jdbcSourceInfo);
        if (conn == null) {
            try {
                releaseDataSource(jdbcSourceInfo);
                DataSource dataSource = getDataSource(jdbcSourceInfo);
                return dataSource.getConnection();
            } catch (Exception e) {
                log.error("Get connection error, jdbcUrl:{}", jdbcSourceInfo.getJdbcUrl());
                throw new SourceException("Get connection error, jdbcUrl:" + jdbcSourceInfo.getJdbcUrl() + " you can try again later or reset datasource");
            }
        }
        return conn;
    }

    private Connection getConnectionWithRetry(JdbcSourceInfo jdbcSourceInfo) {
        int rc = 1;
        for (; ; ) {

            if (rc > 3) {
                return null;
            }

            try {
                Connection connection = getDataSource(jdbcSourceInfo).getConnection();
                if (connection != null && connection.isValid(5)) {
                    return connection;
                }
            } catch (Exception e) {

            }

            try {
                Thread.sleep((long) Math.pow(2, rc) * 1000);
            } catch (InterruptedException e) {

            }

            rc++;
        }
    }

    public static void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Connection release error", e);
            }
        }
    }

    public static void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error("ResultSet close error", e);
            }
        }
    }

    public static boolean checkDriver(String dataSourceName, String jdbcUrl, String version, boolean isExt) {

    	if (StringUtils.isEmpty(dataSourceName) || !LoadSupportDataSourceRunner.getSupportDatasourceMap().containsKey(dataSourceName)) {
            throw new SourceException("Not supported data type: jdbcUrl=" + jdbcUrl);
        }
        
		if (isExt && !StringUtils.isEmpty(version) && !Consts.JDBC_DATASOURCE_DEFAULT_VERSION.equals(version)) {
			String path = System.getenv("FASTBI_HOME") + File.separator
					+ String.format(Consts.PATH_EXT_FORMATTER, dataSourceName, version);
			ExtendedJdbcClassLoader extendedJdbcClassLoader = ExtendedJdbcClassLoader.getExtJdbcClassLoader(path);
			CustomDataSource dataSource = CustomDataSourceUtils.getInstance(jdbcUrl, version);
			try {
				assert extendedJdbcClassLoader != null;
				Class<?> aClass = extendedJdbcClassLoader.loadClass(dataSource.getDriver());
				if (null == aClass) {
					throw new SourceException("Unable to get driver instance for jdbcUrl: " + jdbcUrl);
				}
			} catch (NullPointerException en) {
				throw new ServerException("JDBC driver is not found: " + dataSourceName + ":" + version);
			} catch (ClassNotFoundException ex) {
				throw new SourceException("Unable to get driver instance for jdbcUrl: " + jdbcUrl);
			}
		} else {
			if (DataTypeEnum.ELASTICSEARCH.getDesc().equals(dataSourceName) && !isExt) {
				return true;
			} else {
				try {
					String className = getDriverClassName(jdbcUrl, null);
					Class.forName(className);
				} catch (Exception e) {
					throw new SourceException("Unable to get driver instance for jdbcUrl: " + jdbcUrl, e);
				}
			}
		}
		return true;
    }

    public static String isSupportedDatasource(String jdbcUrl) {
        String dataSourceName = getDataSourceName(jdbcUrl);
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new SourceException("Not supported data type: jdbcUrl=" + jdbcUrl);
        }
        if (!LoadSupportDataSourceRunner.getSupportDatasourceMap().containsKey(dataSourceName)) {
            throw new SourceException("Not supported data type: jdbcUrl=" + jdbcUrl);
        }

        String urlPrefix = String.format(Consts.JDBC_PREFIX_FORMATTER, dataSourceName);
        String checkUrl = jdbcUrl.replaceFirst(Consts.DOUBLE_SLASH, Consts.EMPTY).replaceFirst(Consts.AT_SYMBOL, Consts.EMPTY);
        if (urlPrefix.equals(checkUrl)) {
            throw new SourceException("Communications link failure");
        }

        return dataSourceName;
    }

    public static String getDataSourceName(String jdbcUrl) {
        String dataSourceName = null;
        jdbcUrl = jdbcUrl.replaceAll(Consts.NEW_LINE_CHAR, Consts.EMPTY).replaceAll(Consts.SPACE, Consts.EMPTY).trim();
        Matcher matcher = Consts.PATTERN_JDBC_TYPE.matcher(jdbcUrl);
        if (matcher.find()) {
            dataSourceName = matcher.group().split(Consts.COLON)[1];
        }
        return dataSourceName;
    }

    public static String getDriverClassName(String jdbcUrl, String version) {
        
        String className = null;
        
        try {
            className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
        } catch (SQLException e) {

        }
        
        if (!StringUtils.isEmpty(className) && !className.contains("com.sun.proxy")
                && !className.contains("net.sf.cglib.proxy")) {
            return className;
        }
        
        DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(jdbcUrl);
        CustomDataSource customDataSource = null;
        if (null == dataTypeEnum) {
            try {
                customDataSource = CustomDataSourceUtils.getInstance(jdbcUrl, version);
            }
            catch (Exception e) {
                throw new SourceException(e.getMessage());
            }
        }

        if (null == dataTypeEnum && null == customDataSource) {
            throw new SourceException("Not supported data type: jdbcUrl=" + jdbcUrl);
        }

        return className = null != dataTypeEnum && !StringUtils.isEmpty(dataTypeEnum.getDriver())
                ? dataTypeEnum.getDriver()
                : customDataSource.getDriver().trim();
    }


    /**
     * 释放失效数据源
     *
     * @param jdbcSourceInfo
     * @return
     */
    public void releaseDataSource(JdbcSourceInfo jdbcSourceInfo) {
		jdbcDataSource.removeDatasource(jdbcSourceInfo);
    }

    public static String getKey(String jdbcUrl, String username, String password, String version, boolean isExt) {

        StringBuilder sb = new StringBuilder();
        
        if (!StringUtils.isEmpty(username)) {
            sb.append(username);
        }
        
        if (!StringUtils.isEmpty(password)) {
            sb.append(Consts.COLON).append(password);
        }
        
        sb.append(Consts.AT_SYMBOL).append(jdbcUrl.trim());
        
        if (isExt && !StringUtils.isEmpty(version)) {
            sb.append(Consts.COLON).append(version);
        }

        return MD5Util.getMD5(sb.toString(), true, 64);
    }
}

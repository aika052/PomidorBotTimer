package com.pomidor10bot;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TimerDao {
    private static final String INSERT_QUERY = "insert into timers(user_id, timer_type) values (?, ?)";
    private final DataSource dataSource;

    public TimerDao(DataSource dataSource) {
        this.dataSource=dataSource;
    }
    public void save(Long userId, String timerType) {
        try (Connection connection=dataSource.getConnection()) {
            PreparedStatement preparedStatement=connection.prepareStatement(INSERT_QUERY);
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, timerType);
            preparedStatement.execute();
        } catch (SQLException exception) { throw new IllegalStateException(); }
    }

}

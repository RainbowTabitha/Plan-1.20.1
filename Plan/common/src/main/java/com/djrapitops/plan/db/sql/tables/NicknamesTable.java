/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.db.sql.tables;

import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.data.store.objects.Nickname;
import com.djrapitops.plan.db.DBType;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.ExecStatement;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.patches.NicknameLastSeenPatch;
import com.djrapitops.plan.db.patches.NicknamesOptimizationPatch;
import com.djrapitops.plan.db.patches.Version10Patch;
import com.djrapitops.plan.db.sql.parsing.CreateTableParser;
import com.djrapitops.plan.db.sql.parsing.Sql;
import com.djrapitops.plugin.utilities.Verify;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Table that is in charge of storing nickname data.
 * <p>
 * Table Name: plan_nicknames
 * <p>
 * Patches related to this table:
 * {@link Version10Patch}
 * {@link NicknameLastSeenPatch}
 * {@link NicknamesOptimizationPatch}
 *
 * @author Rsl1122
 */
public class NicknamesTable extends UserUUIDTable {

    public static final String TABLE_NAME = "plan_nicknames";

    public static final String ID = "id";
    public static final String USER_UUID = "uuid";
    public static final String SERVER_UUID = "server_uuid";
    public static final String NICKNAME = "nickname";
    public static final String LAST_USED = "last_used";

    private String insertStatement;
    private final String updateStatement;

    public NicknamesTable(SQLDB db) {
        super(TABLE_NAME, db);
        insertStatement = "INSERT INTO " + tableName + " (" +
                USER_UUID + ", " +
                SERVER_UUID + ", " +
                NICKNAME + ", " +
                LAST_USED +
                ") VALUES (?, ?, ?, ?)";
        updateStatement = "UPDATE " + tableName + " SET " + LAST_USED + "=?" +
                " WHERE " + NICKNAME + "=?" +
                " AND " + USER_UUID + "=?" +
                " AND " + SERVER_UUID + "=?";
    }

    public static String createTableSQL(DBType dbType) {
        return CreateTableParser.create(TABLE_NAME, dbType)
                .column(ID, Sql.INT).primaryKey()
                .column(USER_UUID, Sql.varchar(36)).notNull()
                .column(NICKNAME, Sql.varchar(75)).notNull()
                .column(SERVER_UUID, Sql.varchar(36)).notNull()
                .column(LAST_USED, Sql.LONG).notNull()
                .toString();
    }

    @Override
    public void createTable() throws DBInitException {
        createTable(createTableSQL(db.getType()));
    }

    /**
     * Get nicknames of the user on a server.
     *
     * @param uuid       UUID of the Player
     * @param serverUUID UUID of the server
     * @return The nicknames of the User
     */
    public List<String> getNicknames(UUID uuid, UUID serverUUID) {
        String sql = "SELECT " + NICKNAME + " FROM " + tableName +
                " WHERE (" + USER_UUID + "=?)" +
                " AND " + SERVER_UUID + "=?";

        return query(new QueryStatement<List<String>>(sql, 1000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, uuid.toString());
                statement.setString(2, serverUUID.toString());
            }

            @Override
            public List<String> processResults(ResultSet set) throws SQLException {
                List<String> nicknames = new ArrayList<>();
                while (set.next()) {
                    String nickname = set.getString(NICKNAME);
                    if (nickname.isEmpty()) {
                        continue;
                    }
                    if (!nicknames.contains(nickname)) {
                        nicknames.add(nickname);
                    }
                }
                return nicknames;
            }
        });
    }

    /**
     * Get nicknames of the user on THIS server.
     *
     * @param uuid UUID of the Player
     * @return The nicknames of the User
     */
    public List<String> getNicknames(UUID uuid) {
        return getNicknames(uuid, getServerUUID());
    }

    public Map<UUID, Map<UUID, List<Nickname>>> getAllNicknames() {
        String sql = "SELECT " +
                NICKNAME + ", " +
                LAST_USED + ", " +
                USER_UUID + ", " +
                SERVER_UUID +
                " FROM " + tableName;

        return query(new QueryAllStatement<Map<UUID, Map<UUID, List<Nickname>>>>(sql, 5000) {
            @Override
            public Map<UUID, Map<UUID, List<Nickname>>> processResults(ResultSet set) throws SQLException {
                Map<UUID, Map<UUID, List<Nickname>>> map = new HashMap<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString(SERVER_UUID));
                    UUID uuid = UUID.fromString(set.getString(USER_UUID));

                    Map<UUID, List<Nickname>> serverMap = map.getOrDefault(serverUUID, new HashMap<>());
                    List<Nickname> nicknames = serverMap.getOrDefault(uuid, new ArrayList<>());

                    nicknames.add(new Nickname(
                            set.getString(NICKNAME), set.getLong(LAST_USED), serverUUID
                    ));

                    serverMap.put(uuid, nicknames);
                    map.put(serverUUID, serverMap);
                }
                return map;
            }
        });
    }

    public void saveUserName(UUID uuid, Nickname name) {
        List<Nickname> saved = getNicknameInformation(uuid);
        if (saved.contains(name)) {
            updateNickname(uuid, name);
        } else {
            insertNickname(uuid, name);
        }
    }

    private void updateNickname(UUID uuid, Nickname name) {
        execute(new ExecStatement(updateStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setLong(1, name.getDate());
                statement.setString(2, name.getName());
                statement.setString(3, uuid.toString());
                statement.setString(4, getServerUUID().toString());
            }
        });
    }

    private void insertNickname(UUID uuid, Nickname name) {
        execute(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, uuid.toString());
                statement.setString(2, getServerUUID().toString());
                statement.setString(3, name.getName());
                statement.setLong(4, name.getDate());
            }
        });
    }

    public List<Nickname> getNicknameInformation(UUID uuid) {
        String sql = "SELECT " +
                NICKNAME + ", " +
                LAST_USED + ", " +
                SERVER_UUID +
                " FROM " + tableName +
                " WHERE (" + USER_UUID + "=?)";

        return query(new QueryStatement<List<Nickname>>(sql, 5000) {

            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, uuid.toString());
            }

            @Override
            public List<Nickname> processResults(ResultSet set) throws SQLException {
                List<Nickname> nicknames = new ArrayList<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString(SERVER_UUID));
                    String nickname = set.getString(NICKNAME);
                    nicknames.add(new Nickname(nickname, set.getLong(LAST_USED), serverUUID));
                }
                return nicknames;
            }
        });
    }

    public void insertNicknames(Map<UUID, Map<UUID, List<Nickname>>> allNicknames) {
        if (Verify.isEmpty(allNicknames)) {
            return;
        }

        executeBatch(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                // Every Server
                for (UUID serverUUID : allNicknames.keySet()) {
                    // Every User
                    for (Map.Entry<UUID, List<Nickname>> entry : allNicknames.get(serverUUID).entrySet()) {
                        UUID uuid = entry.getKey();
                        // Every Nickname
                        List<Nickname> nicknames = entry.getValue();
                        for (Nickname nickname : nicknames) {
                            statement.setString(1, uuid.toString());
                            statement.setString(2, serverUUID.toString());
                            statement.setString(3, nickname.getName());
                            statement.setLong(4, nickname.getDate());
                            statement.addBatch();
                        }
                    }
                }
            }
        });
    }
}

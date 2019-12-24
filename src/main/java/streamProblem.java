import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;
import org.postgresql.replication.fluent.logical.ChainedLogicalStreamBuilder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class streamProblem {
  public static void main(String args[]) throws SQLException, InterruptedException {
    System.out.println("Welcome to the first circle.");
    String replicationSlotName = "test_slot";
    String url = "jdbc:postgresql://relay:5432/test_database?loggerLevel=TRACE";
    ByteBuffer msg;
    PGReplicationStream stream;
    LogSequenceNumber lsn;

    Properties connectionProps = new Properties();
    PGProperty.USER.set(connectionProps, "test_user");
    PGProperty.PASSWORD.set(connectionProps, "test_password");
    PGProperty.ASSUME_MIN_SERVER_VERSION.set(connectionProps, "9.4");
    PGProperty.REPLICATION.set(connectionProps, "database");
    PGProperty.PREFER_QUERY_MODE.set(connectionProps, "simple");

    Connection connection = DriverManager.getConnection(url, connectionProps);

    ChainedLogicalStreamBuilder streamBuilder;
    streamBuilder = ((PGConnection) connection).getReplicationAPI()
      .replicationStream()
      .logical()
      .withSlotName(replicationSlotName)
      .withStatusInterval(200, MILLISECONDS);

    stream = streamBuilder.start();

    while (true) {
      msg = stream.readPending();
      lsn = stream.getLastReceiveLSN();
      if (msg == null) {
        Thread.sleep(1000);
      } else {
        int offset = msg.arrayOffset();
        byte[] source = msg.array();
        int length = source.length - offset;
        String decodedMessage = new String(source, offset, length);
        // System.out.println(decodedMessage);
        stream.setAppliedLSN(lsn);
        stream.setFlushedLSN(lsn);
      }
    }
  }
}

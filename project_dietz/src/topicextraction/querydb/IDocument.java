package topicextraction.querydb;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface IDocument {
    int getId();

    Date getDate();

    String getTitle();

    String getDescription();

    String getContributor() throws SQLException;

    String getFormat() throws SQLException;

    String getSource() throws SQLException;

    String getLanguage() throws SQLException;

    //Huiping commented out the following three functions
    //Feb. 14, 2012
    //List<IAuthor> getAuthors();

    //List<Relation> getRelations() throws SQLException;

    //List<Relation> getRelationsByType(int type) throws SQLException;

    String getText();
}

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CommitFinder {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            throw new IllegalArgumentException("The first argument should be an absolute path to a valid git repo, cloned to the disk");
        }

        String path = args[0];
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(path + "/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("commits/" + CommentFinder.getProjectName(path) + ".csv"))) {
            Git git = new Git(repository);
            writer.write("timestamp,commit_hash\n");

            Iterable<RevCommit> commits = git.log().all().call();

            for (RevCommit commit : commits) {
                writer.write(String.format("%s,%s\n", commit.getCommitTime(), commit.getId().getName()));
            }

            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }

}

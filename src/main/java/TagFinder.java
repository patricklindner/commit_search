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

public class TagFinder {

    public static final String DRUID_RELEASE_TAG_NAME = "druid-";
    public static final String COMMON_RELEASE_TAG_NAME = "release-";

    public static final String PULSAR_RELEASE_TAG_NAME = "v";

    public static final String DUBBO_RELEASE_TAG_NAME = "dubbo-";

    public static final String MAVEN_RELEASE_TAG_NAME = "maven-";

    //Change this to the prefix of releases of the repository
    public static final String RELEASE_TAG_NAME = MAVEN_RELEASE_TAG_NAME;

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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CommentFinder.getProjectName(path) + "-tags.csv"))) {
            Git git = new Git(repository);
            writer.write("timestamp,release_name\n");

            RevWalk revWalk = new RevWalk(repository);
            List<Ref> refs = git.tagList().call();

            for (Ref ref : refs) {
                if (ref.getName().startsWith("refs/tags/" + RELEASE_TAG_NAME)) {
                    RevCommit commit = revWalk.parseCommit(ref.getObjectId());
                    writer.write(String.format("%s,%s\n", commit.getCommitTime(), ref.getName()));
                }
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

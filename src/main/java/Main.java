import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Some javadoc
 */
public class Main {

    static Pattern multilineComments = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/");
    static Pattern singleLineComments = Pattern.compile("(\\/\\/.*)");

    public static void main(String[] args) throws IOException, GitAPIException {


        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("/home/patrick/projects/spring-framework/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        try (FileWriter writer = new FileWriter("filename.csv")) {

            ObjectReader objectReader = repository.newObjectReader();

            Git git = new Git(repository);
            Iterable<RevCommit> allCommitsForCounting = git.log().all().call();

            int numberOfCommits = StreamSupport.stream(allCommitsForCounting.spliterator(), false).toList().size();

            Iterable<RevCommit> allCommitsForProcessing = git.log().all().call();

            int currentCommit = 0;
            for (RevCommit commit : allCommitsForProcessing) {
                //TODO: compare to empty string when no parent -> first commit of repo
                if (commit.getParentCount() > 0) {

                    RevCommit parentCommit = commit.getParent(0);

                    try (ObjectReader reader = git.getRepository().newObjectReader()) {
                        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                        oldTreeIter.reset(reader, parentCommit.getTree());
                        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                        newTreeIter.reset(reader, commit.getTree());

                        try (DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                            formatter.setRepository(git.getRepository());

                            List<DiffEntry> diffs = formatter.scan(oldTreeIter, newTreeIter);

                            for (DiffEntry diff : diffs) {

                                String oldLine = "";
                                String newLine = "";
                                if (diff.getChangeType() != DiffEntry.ChangeType.ADD) {
                                    var oldLoader = objectReader.open(diff.getOldId().toObjectId());
                                    oldLine = new String(oldLoader.getBytes(), StandardCharsets.UTF_8);
                                }
                                if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
                                    var newLoader = objectReader.open(diff.getNewId().toObjectId());
                                    newLine = new String(newLoader.getBytes(), StandardCharsets.UTF_8);
                                }

                                DiffMatchPatch dmp = new DiffMatchPatch();
                                LinkedList<DiffMatchPatch.Diff> diffLines = dmp.diffMain(oldLine, newLine);

                                for (DiffMatchPatch.Diff diffBlock : diffLines) {
                                    if (diffBlock.operation != DiffMatchPatch.Operation.EQUAL) {
                                        writeMatchToFile(writer, multilineComments.matcher(diffBlock.text), commit, diffBlock);
                                        writeMatchToFile(writer, singleLineComments.matcher(diffBlock.text), commit, diffBlock);
                                    }
                                }
                            }
                        }
                    }
                }
                System.out.print("\r" + (currentCommit / (float) numberOfCommits) * 100 + " %");
                currentCommit++;
            }
        }
    }

    private static void writeMatchToFile(FileWriter writer, Matcher matcher, RevCommit commit, DiffMatchPatch.Diff diffBlock) throws IOException {
        while (matcher.find()) {
            writer.write(diffBlock.operation + "," + commit.getCommitTime() + "," + commit.getId().getName() + "," + matcher.group() + "\n");
        }
    }
}


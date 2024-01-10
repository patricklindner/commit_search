import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;


public class Main {

    static Pattern multilineComments = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/");
    static Pattern singleLineComments = Pattern.compile("(\\/\\/.*)");

    public static void main(String[] args) throws IOException, GitAPIException {

        if (args.length != 1) {
            throw new IllegalArgumentException("The first argument should be an absolute path to a valid git repo, cloned to the disk");
        }

        String path = args[0];
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(path+"/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        try (FileWriter writer = new FileWriter(getProjectName(path) + ".csv")) {

            ObjectReader objectReader = repository.newObjectReader();

            Git git = new Git(repository);
            Iterable<RevCommit> allCommitsForCounting = git.log().all().call();

            int numberOfCommits = StreamSupport.stream(allCommitsForCounting.spliterator(), false).toList().size();

            Iterable<RevCommit> allCommitsForProcessing = git.log().all().call();

            int currentCommit = 0;
            for (RevCommit commit : allCommitsForProcessing) {

                try (ObjectReader reader = git.getRepository().newObjectReader()) {
                    AbstractTreeIterator oldTreeIter;
                    if (commit.getParentCount() > 0) {
                        RevCommit parentCommit = commit.getParent(0);
                        CanonicalTreeParser old = new CanonicalTreeParser();
                        old.reset(reader, parentCommit.getTree());
                        oldTreeIter = old;
                    } else {
                        oldTreeIter = new EmptyTreeIterator();
                    }
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
                                    writeMatchToFile(writer, multilineComments.matcher(diffBlock.text), commit, diffBlock, true);
                                    writeMatchToFile(writer, singleLineComments.matcher(diffBlock.text), commit, diffBlock, false);
                                }
                            }
                        }
                    }
                }
                //print progress
                System.out.print("\r" + (currentCommit / (float) numberOfCommits) * 100 + " %");
                currentCommit++;
            }
        }
    }

    private static void writeMatchToFile(FileWriter writer, Matcher matcher, RevCommit commit, DiffMatchPatch.Diff diffBlock, boolean multiline) throws IOException {
        while (matcher.find()) {
            String formatted;
            if (multiline) {
                formatted = String.format("%s,%s,%s,\"%s\"\n", diffBlock.operation, commit.getCommitTime(), commit.getId().getName(), matcher.group());
            } else {
                formatted = String.format("%s,%s,%s,%s\n", diffBlock.operation, commit.getCommitTime(), commit.getId().getName(), matcher.group());
            }
            writer.write(formatted);
        }
    }

    private static String getProjectName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}


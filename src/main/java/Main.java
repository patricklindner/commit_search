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
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("/home/patrick/projects/spring-framework/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        FileWriter writer = new FileWriter("filename.txt");

        ObjectReader objectReader = repository.newObjectReader();

        Git git = new Git(repository);
        Iterable<RevCommit> allCommits = git.log().all().call();


        for (RevCommit commit : allCommits) {

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
                                oldLine = new String(oldLoader.getBytes());
                            }
                            if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
                                var newLoader = objectReader.open(diff.getNewId().toObjectId());
                                newLine = new String(newLoader.getBytes());
                            }

                            DiffMatchPatch dmp = new DiffMatchPatch();
                            LinkedList<DiffMatchPatch.Diff> diffLines = dmp.diffMain(oldLine, newLine);

                            for (DiffMatchPatch.Diff diffLine : diffLines) {
                                if(diffLine.operation != DiffMatchPatch.Operation.EQUAL) {
                                    writer.write(diffLine.operation+","+commit.getCommitTime()+","+diffLine.text+"\n");
                                }
                            }

                        }
                    }
                }

            }
        }

//                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
//                diffFormatter.setRepository(repository);
//                CanonicalTreeParser oldTreeParser = new CanonicalTreeParser(null, objectReader, parentCommit.getTree());
//                CanonicalTreeParser newTreeParser = new CanonicalTreeParser(null, objectReader, commit.getTree());
//                List<DiffEntry> diffs = diffFormatter.scan(oldTreeParser, newTreeParser);
//
//                for (DiffEntry diff : diffs) {
//                    String oldPath = diff.getOldPath();
//                    String newPath = diff.getNewPath();
//                    newTreeParser.findFile(newPath);
//                    oldTreeParser.findFile(oldPath);
//
//                    ObjectLoader newLoader = objectReader.open(newTreeParser.getEntryObjectId());
//                    ObjectLoader oldLoader = objectReader.open(oldTreeParser.getEntryObjectId());
//
//                    DiffMatchPatch dmp = new DiffMatchPatch();
//                    LinkedList<DiffMatchPatch.Diff> diffs1 = dmp.diffMain(new String(oldLoader.getBytes()), new String(newLoader.getBytes()));
//                }

    }

}


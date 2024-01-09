import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
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
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("/Users/Patrick.Lindner/IdeaProjects/spring-framework/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        ObjectReader objectReader = repository.newObjectReader();

        Git git = new Git(repository);
        Iterable<RevCommit> allCommits = git.log().all().call();

        RevCommit commit = allCommits.iterator().next();

        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setRepository(repository);
        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser(null, repository.newObjectReader(), commit.getParent(0).getTree());
        CanonicalTreeParser newTreeParser = new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree());
        List<DiffEntry> diffs = diffFormatter.scan(oldTreeParser, newTreeParser);

        DiffEntry diffEntry = diffs.get(0);
        String newPath = diffEntry.getNewPath();
        newTreeParser.findFile(newPath);

        ObjectLoader loader = objectReader.open(newTreeParser.getEntryObjectId());
        System.out.println(diffEntry.getNewPath());
        loader.copyTo(System.out);

//
//        EditList edits = diffFormatter.toFileHeader(diffs.get(0)).toEditList();
//
//        edits.get(0).
//
//        ObjectLoader open = objectReader.open(commit);
//
//
//        System.out.println();
    }
}
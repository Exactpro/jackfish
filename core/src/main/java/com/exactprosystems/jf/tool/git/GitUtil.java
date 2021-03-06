/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactprosystems.jf.tool.git;

import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.git.merge.editor.Chunk;
import com.exactprosystems.jf.tool.git.pull.GitPullBean;
import com.exactprosystems.jf.tool.git.reset.FileWithStatusBean;
import com.exactprosystems.jf.tool.git.reset.GitResetBean;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeChunk;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.StrategyResolve;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.jgit.lib.Constants.*;

public class GitUtil
{
	public static final CredentialBean EMPTY_BEAN = new CredentialBean("", "", "", "");

	private GitUtil()
	{

	}

	public static boolean isGitRepository() throws Exception
	{
		try
		{
			Repository build = new FileRepositoryBuilder().findGitDir(new File(new File(".").getAbsolutePath())).build();
			return build != null;
		}
		catch (IllegalArgumentException e)
		{
			//we catch the exception, if git repository not found
		}
		return false;
	}

	public static File gitRootDirectory(CredentialBean credentialBean) throws Exception
	{
		try (Git git = git(credentialBean))
		{
			return git.getRepository().getWorkTree();
		}
	}

	//region Clone
	public static void gitClone(String remotePath, File projectFolder, CredentialBean credentials, ProgressMonitor monitor) throws Exception
	{
		CredentialsProvider credentialsProvider = getCredentialsProvider(credentials);
		try (Git git = Git.cloneRepository().setURI(remotePath).setDirectory(projectFolder).setCredentialsProvider(credentialsProvider).setProgressMonitor(monitor).call())
		{
			;
		}
	}
	//endregion

	//region Commit
	public static void gitCommit(CredentialBean bean, List<File> files, String msg, boolean isAmend) throws Exception
	{
		try (Git git = git(bean))
		{
			AddCommand add = git.add();
			files.stream().map(File::getPath).map(Common::getRelativePath).forEach(add::addFilepattern);
			add.call();
			git.commit().setAmend(isAmend).setMessage(msg).call();
		}
	}

	public static void gitPush(CredentialBean bean, String remoteBranchName) throws Exception
	{
		try (Git git = git(bean))
		{
			Iterable<PushResult> call = git.push().setRefSpecs(new RefSpec(git.getRepository().getFullBranch() + ":" + remoteBranchName)).setCredentialsProvider(getCredentialsProvider(bean)).call();
			for (PushResult pushResult : call)
			{
				for (RemoteRefUpdate update : pushResult.getRemoteUpdates())
				{
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=478199#c2
					switch (update.getStatus())
					{
						case NOT_ATTEMPTED:
							break;
						case UP_TO_DATE:
							break;
						case REJECTED_NONFASTFORWARD:
							throw new Exception(R.GIT_UTIL_GIT_PUSH_EXCEPTION.get());
						case REJECTED_NODELETE:
							break;
						case REJECTED_REMOTE_CHANGED:
							break;
						case REJECTED_OTHER_REASON:
							break;
						case NON_EXISTING:
							break;
						case AWAITING_REPORT:
							break;
						case OK:
							break;
					}
				}
			}
		}
	}

	public static List<GitResetBean> gitUnpushingCommits(CredentialBean credentialBean) throws Exception
	{
		try (Git git = git(credentialBean))
		{
			LogCommand log = git.log();
			Repository repo = git.getRepository();
			log.addRange(repo.exactRef(R_REMOTES + DEFAULT_REMOTE_NAME + "/" + MASTER).getObjectId(), repo.exactRef(Constants.HEAD).getObjectId());
			Iterable<RevCommit> call = log.call();

			List<GitResetBean> list = new ArrayList<>();
			for (RevCommit revCommit : call)
			{
				list.add(new GitResetBean(revCommit, credentialBean));
			}
			return list;
		}
	}
	//endregion

	//region Pull
	public static List<GitPullBean> gitPull(CredentialBean bean, ProgressMonitor monitor, String remoteBranchName) throws Exception
	{
		try (Git git = git(bean))
		{
			ObjectId oldHead = git.getRepository().resolve("HEAD^{tree}");

			List<GitPullBean> list = new ArrayList<>();
			try
			{
				PullResult pullResult = git.pull().
						setCredentialsProvider(getCredentialsProvider(bean)).setProgressMonitor(monitor).setRemoteBranchName(remoteBranchName).call();

				//get merging files
				MergeResult mergeResult = pullResult.getMergeResult();
				if (mergeResult != null)
				{
					Map<String, int[][]> allConflicts = mergeResult.getConflicts();
					if (allConflicts != null)
					{
						list.addAll(allConflicts.keySet().stream().map(path -> new GitPullBean(path, true)).collect(Collectors.toList()));
					}
				}

				//get fetching files
				FetchResult fetchResult = pullResult.getFetchResult();
				Collection<TrackingRefUpdate> trackingRefUpdates = fetchResult.getTrackingRefUpdates();
				String fullBranch = git.getRepository().getFullBranch();
				for (TrackingRefUpdate update : trackingRefUpdates)
				{
					if (!update.getRemoteName().equals(fullBranch))
					{
						continue;
					}
					ObjectId oldObjectId = update.getOldObjectId();
					ObjectId newObjectId = update.getNewObjectId();

					ObjectId oldTreeId = git.getRepository().resolve(oldObjectId.name() + "^{tree}");
					ObjectId newTreeId = git.getRepository().resolve(newObjectId.name() + "^{tree}");

					List<DiffEntry> diffEntries = getDiffEntries(git, oldTreeId, newTreeId);
					for (DiffEntry diff : diffEntries)
					{
						DiffEntry.ChangeType changeType = diff.getChangeType();
						String fileName = changeType == DiffEntry.ChangeType.DELETE ? diff.getOldPath() : diff.getNewPath();
						GitPullBean pullBean = new GitPullBean(fileName, false);
						if (!list.contains(pullBean))
						{
							list.add(pullBean);
						}
					}
				}
				return list;
			}
			catch (CheckoutConflictException cce)
			{
				throw new Exception(R.GIT_UTIL_GIT_PULL_EXCEPTION.get() + cce.getConflictingPaths().toString());
			}
		}
	}

	public static String checkFile(CredentialBean bean, String filePath) throws Exception
	{
		if (new File(filePath).exists())
		{
			return replaceWinSeparatorToUnux(filePath);
		}
		try (Git git = git(bean))
		{
			String pathToGitFolder = git.getRepository().getDirectory().getParent();
			return replaceWinSeparatorToUnux(pathToGitFolder + File.separator + filePath);
		}
	}

	private static List<DiffEntry> getDiffEntries(Git git, ObjectId oldHead, ObjectId head) throws IOException, GitAPIException
	{
		ObjectReader reader = git.getRepository().newObjectReader();

		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, oldHead);

		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, head);

		return git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
	}

	public static List<Chunk> getConflicts(CredentialBean bean, String fileName) throws Exception
	{
		try (Git git = git(bean))
		{
			Repository repo = git.getRepository();
			ThreeWayMerger merger = new StrategyResolve().newMerger(repo, true);
			merger.merge(repo.resolve(Constants.HEAD), repo.resolve(Constants.FETCH_HEAD));
			ResolveMerger resolveMerger = (ResolveMerger) merger;

			Map<String, org.eclipse.jgit.merge.MergeResult<?>> mergeResults = resolveMerger.getMergeResults();

			org.eclipse.jgit.merge.MergeResult<?> mergeChunks = mergeResults.get(fileName);
			if (mergeChunks == null)
			{
				return null;
			}
			if (!mergeChunks.containsConflicts())
			{
				return null;
			}
			List<Chunk> lines = new ArrayList<>();
			for (MergeChunk mergeChunk : mergeChunks)
			{
				MergeChunk.ConflictState conflictState = mergeChunk.getConflictState();
				switch (conflictState)
				{
					case NO_CONFLICT:
						lines.add(new Chunk(false, mergeChunk.getBegin(), mergeChunk.getEnd(), null));
						break;
					case FIRST_CONFLICTING_RANGE:
						lines.add(new Chunk(true, mergeChunk.getBegin(), mergeChunk.getEnd(), Chunk.ChunkState.Your));
						break;
					case NEXT_CONFLICTING_RANGE:
						lines.add(new Chunk(true, mergeChunk.getBegin(), mergeChunk.getEnd(), Chunk.ChunkState.Their));
						break;
				}
			}

			return lines;
		}
	}
	//endregion

	//region Reset
	public static void gitReset(CredentialBean bean, String ref) throws Exception
	{
		try (Git git = git(bean))
		{
			git.reset().setRef(ref).setMode(ResetCommand.ResetType.HARD).call();
		}
	}

	public static List<FileWithStatusBean> getCommitFiles(CredentialBean bean, RevCommit commit) throws Exception
	{
		try (Git git = git(bean))
		{
			List<FileWithStatusBean> list = new ArrayList<>();

			ObjectId currentId = commit.getTree().getId();
			ObjectId parentId = commit.getTree().getId();
			if (commit.getParentCount() != 0)
			{
				parentId = commit.getParent(0).getTree().getId();
			}

			List<DiffEntry> diffs = getDiffEntries(git, parentId, currentId);
			list.addAll(diffs.stream().map(FileWithStatusBean::new).collect(Collectors.toList()));

			return list;
		}
	}

	public static List<GitResetBean> gitGetCommits(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			List<GitResetBean> list = new ArrayList<>();
			Iterable<RevCommit> commits = git.log().all().call();
			for (RevCommit commit : commits)
			{
				list.add(new GitResetBean(commit, bean));
			}
			return list;
		}
	}
	//endregion

	//region Status
	public static void revertPaths(CredentialBean bean, Set<String> paths) throws Exception
	{
		try (Git git = git(bean))
		{
			CheckoutCommand checkout = git.checkout();
			paths.stream().map(GitUtil::replaceWinSeparatorToUnux).forEach(checkout::addPath);
			checkout.call();
		}
	}

	public static void ignorePaths(List<String> paths) throws Exception
	{
		File gitIgnore = checkGitIgnoreFile();
		try (FileWriter writer = new FileWriter(gitIgnore, true))
		{
			for (String path : paths)
			{
				writer.write(replaceWinSeparatorToUnux(path) + "\n");
			}
		}
	}

	public static List<GitBean> gitStatus(CredentialBean credential) throws Exception
	{
		try (Git git = git(credential))
		{
			Status status = git.status().call();
			ArrayList<GitBean> list = new ArrayList<>();
			//get all files. uncommitted contains all files without untracked
			status.getUncommittedChanges().stream().map(st -> new GitBean(GitBean.Status.UNSTAGED, new File(st))).forEach(list::add);
			status.getUntracked().stream().map(st -> new GitBean(GitBean.Status.UNTRACKED, new File(st))).forEach(list::add);

			replaceFiles(list, status.getAdded(), GitBean.Status.ADDED);
			replaceFiles(list, status.getChanged(), GitBean.Status.CHANGED);
			replaceFiles(list, status.getRemoved(), GitBean.Status.REMOVED);
			replaceFiles(list, status.getConflicting(), GitBean.Status.CONFLICTING);
			list.sort(Comparator.comparing(GitBean::getStatus));
			List<String> collect = status.getIgnoredNotInIndex().stream().map(File::new).map(File::getAbsolutePath).collect(Collectors.toList());
			list.removeIf(bean -> collect.contains(bean.getFile().getAbsolutePath()));
			return list;
		}
	}

	public static String gitState(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			return git.getRepository().getRepositoryState().getDescription();
		}
	}
	//endregion

	//region Merge
	public static List<String> getTheirs(CredentialBean bean, String filePath) throws Exception
	{
		try (Git git = git(bean))
		{
			Repository repository = git.getRepository();
			ObjectId lastCommitId = repository.resolve(Constants.MERGE_HEAD);
			try (RevWalk revWalk = new RevWalk(repository))
			{
				RevCommit commit = revWalk.parseCommit(lastCommitId);
				RevTree tree = commit.getTree();
				try (TreeWalk treeWalk = new TreeWalk(repository))
				{
					treeWalk.addTree(tree);
					treeWalk.setRecursive(true);
					treeWalk.setFilter(PathFilter.create(replaceWinSeparatorToUnux(filePath)));
					if (!treeWalk.next())
					{
						return Collections.emptyList();
					}
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repository.open(objectId);
					File tmpFile = File.createTempFile("temp", null);
					try (FileOutputStream fos = new FileOutputStream(tmpFile))
					{
						loader.copyTo(fos);
					}
					return Common.readFile(tmpFile, true);
				}
			}
		}
	}

	public static void mergeTheirs(CredentialBean credentialBean, String filePath) throws Exception
	{
		filePath = replaceWinSeparatorToUnux(filePath);
		List<String> theirs = getTheirs(credentialBean, filePath);
		Common.writeToFile(new File(GitUtil.checkFile(credentialBean, filePath)), theirs);
		try (Git git = git(credentialBean))
		{
			git.add().addFilepattern(filePath).call();
		}

	}

	public static List<String> getYours(CredentialBean bean, String filePath) throws Exception
	{
		try (Git git = git(bean))
		{
			Repository repository = git.getRepository();
			ObjectId lastCommitId = repository.resolve(Constants.HEAD);
			try (RevWalk revWalk = new RevWalk(repository))
			{
				RevCommit commit = revWalk.parseCommit(lastCommitId);
				RevTree tree = commit.getTree();
				try (TreeWalk treeWalk = new TreeWalk(repository))
				{
					treeWalk.addTree(tree);
					treeWalk.setRecursive(true);
					treeWalk.setFilter(PathFilter.create(replaceWinSeparatorToUnux(filePath)));
					if (!treeWalk.next())
					{
						return Collections.emptyList();
					}
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repository.open(objectId);
					File tmpFile = File.createTempFile("temp", null);
					try (FileOutputStream fos = new FileOutputStream(tmpFile))
					{
						loader.copyTo(fos);
					}
					return Common.readFile(tmpFile, true);
				}
			}
		}
	}

	public static void mergeYours(CredentialBean credentialBean, String filePath) throws Exception
	{
		filePath = replaceWinSeparatorToUnux(filePath);
		List<String> yours = getYours(credentialBean, filePath);
		Common.writeToFile(new File(GitUtil.checkFile(credentialBean, filePath)), yours);
		try (Git git = git(credentialBean))
		{
			git.add().addFilepattern(filePath).call();
		}
	}

	public static void addFileToIndex(CredentialBean bean, String filePath) throws Exception
	{
		try (Git git = git(bean))
		{
			git.add().addFilepattern(replaceWinSeparatorToUnux(filePath)).call();
		}
	}
	//endregion

	//region Branch
	public static String currentBranch(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			return git.getRepository().getBranch();
		}
	}

	public static List<Branch> getBranches(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			String currentBranch = git.getRepository().getFullBranch();
			List<Ref> localBranches = git.branchList().call();
			List<Ref> remoteBrahcnes = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
			List<Branch> list = new ArrayList<>();

			localBranches.stream().filter(r -> !r.isSymbolic()).map(Ref::getName).map(name -> new Branch(true, false, name)).peek(b -> b.isCurrent = b.name.equals(currentBranch)).forEach(list::add);

			remoteBrahcnes.stream().filter(r -> !r.isSymbolic()).map(Ref::getName).map(name -> new Branch(false, false, name)).forEach(list::add);

			return list;
		}
	}

	public static String getRemoteBranch(CredentialBean bean, String localBranch) throws Exception
	{
		try (Git git = git(bean))
		{
			String string = git.getRepository().getConfig().getString("branch", localBranch, "merge");
			return string == null ? localBranch : string.replace("refs/heads/", "");
		}
	}

	public static void createNewBranch(CredentialBean bean, String newName) throws Exception
	{
		try (Git git = git(bean))
		{
			Ref ref = git.branchCreate().setName(newName)
					//think how to set upstream correctly
					.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).call();

			Iterable<PushResult> call = git.push().setCredentialsProvider(getCredentialsProvider(bean)).add(ref).call();
		}
	}

	public static String checkout(CredentialBean bean, String branchName, String localBranchName) throws Exception
	{
		try (Git git = git(bean))
		{
			CheckoutCommand checkoutCommand = git.checkout().setStartPoint(branchName).setCreateBranch(false).setName(branchName);

			if (localBranchName != null)
			{
				checkoutCommand.setName(localBranchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setCreateBranch(true);
			}
			Ref ref = checkoutCommand.call();
			return ref.getName();
		}
	}

	public static void rename(CredentialBean bean, String oldName, String newName) throws Exception
	{
		try (Git git = git(bean))
		{
			git.branchRename().setNewName(newName).setOldName(oldName).call();
		}
	}

	public static void deleteBranch(CredentialBean bean, Branch branch) throws Exception
	{
		try (Git git = git(bean))
		{
			if (branch.isLocal())
			{
				git.branchDelete().setBranchNames(branch.getFullName()).setForce(true).call();
			}
			else
			{
				git.push().setCredentialsProvider(getCredentialsProvider(bean)).setRefSpecs(new RefSpec(":refs/heads/" + branch.getSimpleName())).call();
			}
		}
	}

	public static class Branch
	{
		private boolean isLocal;
		private boolean isCurrent;
		private String  name;

		public Branch(boolean isLocal, boolean isCurrent, String name)
		{
			this.isLocal = isLocal;
			this.isCurrent = isCurrent;
			this.name = name;
		}

		public String getFullName()
		{
			return this.name;
		}

		public boolean isLocal()
		{
			return isLocal;
		}

		public boolean isCurrent()
		{
			return isCurrent;
		}

		public String getSimpleName()
		{
			return this.name.substring(this.name.lastIndexOf("/") + 1);
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Branch branch = (Branch) o;

			return name.equals(branch.name);
		}

		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
	}
	//endregion

	//region Tags
	public static List<Tag> getTags(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			List<Ref> call = git.tagList().call();
			return call.stream().map(Ref::getName).map(Tag::new).collect(Collectors.toList());
		}
	}

	public static void deleteTag(CredentialBean bean, String tagName) throws Exception
	{
		try (Git git = git(bean))
		{
			git.tagDelete().setTags(tagName).call();
		}
	}

	public static void newTag(CredentialBean bean, String message, String version) throws Exception
	{
		try (Git git = git(bean))
		{
			git.tag().setMessage(message).setName(version).call();
		}
	}

	public static void pushTag(CredentialBean bean) throws Exception
	{
		try (Git git = git(bean))
		{
			git.push().setPushTags().setCredentialsProvider(getCredentialsProvider(bean)).call();
		}
	}

	public static class Tag
	{
		private String name;
		private String fullname;

		public Tag(String fullname)
		{
			this.fullname = fullname;
			this.name = this.fullname.replace("refs/tags/", "");
		}

		public String getName()
		{
			return name;
		}

		public String getFullname()
		{
			return fullname;
		}
	}
	//endregion

	public static void rmFile(File file) throws Exception
	{
		try (Git git = git(EMPTY_BEAN))
		{
			git.rm().addFilepattern(convertFilePathToGitFilePath(git, file)).call();
		}
	}

	public static void rmFiles(List<File> files) throws Exception
	{
		try (Git git = git(EMPTY_BEAN))
		{
			RmCommand rm = git.rm();
			files.stream()
					.map(file -> Common.tryCatch(() -> convertFilePathToGitFilePath(git, file), "", null))
					.filter(Objects::nonNull)
					.forEach(rm::addFilepattern);
			rm.call();
		}
	}

	//region private methods
	private static String replaceWinSeparatorToUnux(String path)
	{
		return path.replace('\\', '/');
	}

	private static String convertFilePathToGitFilePath(Git git, File file) throws Exception
	{
		String repositoryPath = replaceWinSeparatorToUnux(git.getRepository().getWorkTree().getPath());
		String filePath = file.getPath();
		filePath = replaceWinSeparatorToUnux(filePath);
		if (filePath.contains(repositoryPath))
		{
			filePath = filePath.substring(repositoryPath.length() + 1);
		}
		else
		{
			filePath = checkFile(EMPTY_BEAN, file.getPath());
		}
		return filePath;
	}

	private static File checkGitIgnoreFile() throws Exception
	{
		File rootDirectory = gitRootDirectory(EMPTY_BEAN);
		File file = new File(rootDirectory + File.separator + Constants.GITIGNORE_FILENAME);
		if (!file.exists())
		{
			file.createNewFile();
		}
		return file;
	}

	private static void replaceFiles(List<GitBean> mainList, Set<String> newList, GitBean.Status status)
	{
		List<GitBean> collect = newList.stream()
				.map(GitUtil::replaceWinSeparatorToUnux)
				.map(st -> new GitBean(status, new File(st)))
				.collect(Collectors.toList());
		mainList.stream().filter(collect::contains).forEach(bean -> bean.updateStatus(status));
	}

	private static Git git(CredentialBean bean) throws Exception
	{
		setSSH(bean);
		Repository localRepo = new FileRepositoryBuilder().findGitDir().build();
		return new Git(localRepo);
	}

	private static void setSSH(CredentialBean credentials)
	{
		CustomJschConfigSessionFactory jschConfigSessionFactory = new CustomJschConfigSessionFactory(credentials);
		if (jschConfigSessionFactory.isValid())
		{
			SshSessionFactory.setInstance(jschConfigSessionFactory);
		}
	}

	private static CredentialsProvider getCredentialsProvider(CredentialBean credentials)
	{
		return new CredentialsProvider()
		{
			@Override
			public boolean isInteractive()
			{
				return true;
			}

			@Override
			public boolean supports(CredentialItem... credentialItems)
			{
				return true;
			}

			@Override
			public boolean get(URIish urIish, CredentialItem... credentialItems) throws UnsupportedCredentialItem
			{
				for (CredentialItem item : credentialItems)
				{
					//for github
					if (item instanceof CredentialItem.Username)
					{
						((CredentialItem.StringType) item).setValue(credentials.getUsername());
					}
					else if (item instanceof CredentialItem.Password)
					{
						((CredentialItem.Password) item).setValue(credentials.getPassword().toCharArray());
					}
					//for ssh
					else if (item instanceof CredentialItem.StringType)
					{
						((CredentialItem.StringType) item).setValue(credentials.getPassword());
					}
					//this need for ssh on windows
					else if (item instanceof CredentialItem.YesNoType)
					{
						((CredentialItem.YesNoType) item).setValue(true);
					}
				}
				return true;
			}
		};
	}

	private static class CustomJschConfigSessionFactory extends JschConfigSessionFactory
	{
		private boolean isValid = true;
		private String pathToIdRsa;
		private String pathToKnownHosts;

		public CustomJschConfigSessionFactory(CredentialBean credentials)
		{
			String pathToRsa = credentials.getPathToRsa();
			String pathToHosts = credentials.getPathToHosts();
			this.isValid = !Str.IsNullOrEmpty(pathToHosts) && !Str.IsNullOrEmpty(pathToRsa);
			if (this.isValid)
			{
				this.pathToIdRsa = pathToRsa;
				this.pathToKnownHosts = pathToHosts;
			}
		}

		public boolean isValid()
		{
			return isValid;
		}

		@Override
		protected void configure(OpenSshConfig.Host host, Session session)
		{
			session.setConfig("StrictHostKeyChecking", "yes");
		}

		@Override
		protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException
		{
			JSch jsch = super.getJSch(hc, fs);
			jsch.removeAllIdentity();
			jsch.addIdentity(this.pathToIdRsa);
			jsch.setKnownHosts(this.pathToKnownHosts);
			return jsch;
		}
	}
	//endregion
}

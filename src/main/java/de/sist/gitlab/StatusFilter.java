package de.sist.gitlab;

import com.intellij.openapi.project.Project;
import de.sist.gitlab.config.PipelineViewerConfig;
import git4idea.GitReference;
import git4idea.branch.GitBranchesCollection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("RedundantIfStatement")
public class StatusFilter {

    private final PipelineViewerConfig config;
    private final GitlabService gitlabService;

    public StatusFilter(Project project) {
        config = PipelineViewerConfig.getInstance(project);
        gitlabService = project.getService(GitlabService.class);
    }

    public List<PipelineJobStatus> filterPipelines(List<PipelineJobStatus> toFilter) {
        GitBranchesCollection branches = gitlabService.getCurrentRepository().getBranches();
        Set<String> trackedBranches = branches.getLocalBranches().stream().filter(x -> branches.getRemoteBranches().stream().anyMatch(remote -> remote.getNameForRemoteOperations().equals(x.getName()))).map(GitReference::getName).collect(Collectors.toSet());

        return toFilter.stream().filter(x -> {
                    if (!config.getStatusesToWatch().contains(x.result)) {
                        return false;
                    }
                    if (config.getBranchesToIgnore().contains(x.branchName)) {
                        return false;
                    }
                    if (trackedBranches.contains(x.branchName)) {
                        return true;
                    }
                    if (config.getBranchesToWatch().contains(x.branchName)) {
                        return true;
                    }
                    return false;
                }
        ).collect(Collectors.toList());
    }


}
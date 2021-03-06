package de.sist.gitlab.git;

import com.intellij.util.messages.Topic;
import git4idea.repo.GitRepository;

public interface GitInitListener {

    Topic<GitInitListener> GIT_INITIALIZED = Topic.create(".git initialized", GitInitListener.class);

    void handle(GitRepository gitRepository);

}

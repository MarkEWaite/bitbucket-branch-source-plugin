/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.bitbucket.api;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Represents a pull request source, which is a repository, a branch in that repository and the head commit.
 */
public interface BitbucketPullRequestSource {

    /**
     * @return source repository
     */
    BitbucketRepository getRepository();

    /**
     * @return source branch to be merged in the pull request
     */
    BitbucketBranch getBranch();

    /**
     * Returns the type of the source branch.
     * <p>
     * In some server implementations the source HEAD could be a tag.
     *
     * @return the source branch type
     */
    @NonNull
    PullRequestBranchType getBranchType();

    /**
     * @return the branch head commit
     */
    BitbucketCommit getCommit();

}

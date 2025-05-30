/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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
package com.cloudbees.jenkins.plugins.bitbucket.trait;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSourceContext;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSourceRequest;
import com.cloudbees.jenkins.plugins.bitbucket.BranchSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.Messages;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketPullRequest;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepository;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import java.io.IOException;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.trait.SCMHeadAuthority;
import jenkins.scm.api.trait.SCMHeadAuthorityDescriptor;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.impl.trait.Discovery;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A {@link Discovery} trait for bitbucket that will discover branches on the repository.
 *
 * @since 2.2.0
 */
public class BranchDiscoveryTrait extends SCMSourceTrait {
    /**
     * The strategy encoded as a bit-field.
     * <dl>
     *     <dt>Bit 0</dt>
     *     <dd>Build branches that are not filed as a PR</dd>
     *     <dt>Bit 1</dt>
     *     <dd>Build branches that are filed as a PR</dd>
     * </dl>
     */
    private final int strategyId;

    /**
     * Constructor for stapler.
     *
     * @param strategyId the strategy id.
     */
    @DataBoundConstructor
    public BranchDiscoveryTrait(int strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * Constructor for legacy code.
     *
     * @param buildBranch       build branches that are not filed as a PR.
     * @param buildBranchWithPr build branches that are also PRs.
     */
    public BranchDiscoveryTrait(boolean buildBranch, boolean buildBranchWithPr) {
        this.strategyId = (buildBranch ? 1 : 0) + (buildBranchWithPr ? 2 : 0);
    }

    /**
     * Returns the strategy id.
     *
     * @return the strategy id.
     */
    public int getStrategyId() {
        return strategyId;
    }

    /**
     * Returns {@code true} if building branches that are not filed as a PR.
     *
     * @return {@code true} if building branches that are not filed as a PR.
     */
    @Restricted(NoExternalUse.class)
    public boolean isBuildBranch() {
        return (strategyId & 1) != 0;
    }

    /**
     * Returns {@code true} if building branches that are filed as a PR.
     *
     * @return {@code true} if building branches that are filed as a PR.
     */
    @Restricted(NoExternalUse.class)
    public boolean isBuildBranchesWithPR() {
        return (strategyId & 2) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        BitbucketSCMSourceContext ctx = (BitbucketSCMSourceContext) context;
        ctx.wantBranches(true);
        ctx.withAuthority(new BranchSCMHeadAuthority());
        switch (strategyId) {
            case 1:
                ctx.wantOriginPRs(true);
                ctx.withFilter(new ExcludeOriginPRBranchesSCMHeadFilter());
                break;
            case 2:
                ctx.wantOriginPRs(true);
                ctx.withFilter(new OnlyOriginPRBranchesSCMHeadFilter());
                break;
            case 3:
            default:
                // we don't care if it is a PR or not, we're taking them all, no need to ask for PRs and no need
                // to filter
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category.isUncategorized();
    }

    /**
     * Our descriptor.
     */
    @Symbol("bitbucketBranchDiscovery")
    @Extension
    @Discovery
    public static class DescriptorImpl extends BitbucketSCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.BranchDiscoveryTrait_displayName();
        }

        /**
         * Populates the strategy options.
         *
         * @return the strategy options.
         */
        @NonNull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillStrategyIdItems() {
            ListBoxModel result = new ListBoxModel();
            result.add(Messages.BranchDiscoveryTrait_excludePRs(), "1");
            result.add(Messages.BranchDiscoveryTrait_onlyPRs(), "2");
            result.add(Messages.BranchDiscoveryTrait_allBranches(), "3");
            return result;
        }
    }

    /**
     * Trusts branches from the origin repository.
     */
    public static class BranchSCMHeadAuthority extends SCMHeadAuthority<SCMSourceRequest, BranchSCMHead, SCMRevision> {
        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull BranchSCMHead head) {
            return true;
        }

        /**
         * Out descriptor.
         */
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDisplayName() {
                return "Trust origin branches";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Default.class.isAssignableFrom(originClass);
            }
        }
    }

    /**
     * Filter that excludes branches that are also filed as a pull request.
     */
    public static class ExcludeOriginPRBranchesSCMHeadFilter extends SCMHeadFilter {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            if (head instanceof BranchSCMHead && request instanceof BitbucketSCMSourceRequest) {
                BitbucketSCMSourceRequest req = (BitbucketSCMSourceRequest) request;
                String fullName = req.getRepoOwner() + "/" + req.getRepository();
                try {
                    for (BitbucketPullRequest pullRequest : req.getPullRequests()) {
                        BitbucketRepository source = pullRequest.getSource().getRepository();
                        if (StringUtils.equalsIgnoreCase(fullName, source.getFullName())
                                && pullRequest.getSource().getBranch().getName().equals(head.getName())) {
                            request.listener().getLogger().println("Discard branch " + head.getName()
                                    + " because current strategy excludes branches that are also filed as a pull request");
                            return true;
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    // should never happens because data in the requests has been already initialised
                    e.printStackTrace(request.listener().getLogger());
                }
            }
            return false;
        }
    }

    /**
     * Filter that excludes branches that are not also filed as a pull request.
     */
    public static class OnlyOriginPRBranchesSCMHeadFilter extends SCMHeadFilter {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            if (head instanceof BranchSCMHead && request instanceof BitbucketSCMSourceRequest) {
                BitbucketSCMSourceRequest req = (BitbucketSCMSourceRequest) request;
                String fullName = req.getRepoOwner() + "/" + req.getRepository();
                try {
                    for (BitbucketPullRequest pullRequest : req.getPullRequests()) {
                        BitbucketRepository source = pullRequest.getSource().getRepository();
                        if (fullName.equalsIgnoreCase(source.getFullName())
                                && pullRequest.getSource().getBranch().getName().equals(head.getName())) {
                            return false;
                        }
                    }
                    request.listener().getLogger().println("Discard branch " + head.getName()
                            + " because current strategy excludes branches that are not also filed as a pull request");
                    return true;
                } catch (IOException | InterruptedException e) {
                    // should never happens because data in the requests has been already initialised
                    e.printStackTrace(request.listener().getLogger());
                }
            }
            return false;
        }
    }
}

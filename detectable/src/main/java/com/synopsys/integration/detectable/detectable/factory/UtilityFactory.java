package com.synopsys.integration.detectable.detectable.factory;

import com.synopsys.integration.detectable.detectable.executable.impl.CachedExecutableResolverOptions;
import com.synopsys.integration.detectable.detectable.executable.impl.SimpleExecutableFinder;
import com.synopsys.integration.detectable.detectable.executable.impl.SimpleExecutableResolver;
import com.synopsys.integration.detectable.detectable.executable.impl.SimpleLocalExecutableFinder;
import com.synopsys.integration.detectable.detectable.executable.impl.SimpleSystemExecutableFinder;
import com.synopsys.integration.detectable.detectable.file.FileFinder;
import com.synopsys.integration.detectable.detectable.file.impl.SimpleFileFinder;

public class UtilityFactory {
    public FileFinder simpleFileFinder() {
        return new SimpleFileFinder();
    }

    public SimpleExecutableFinder simpleExecutableFinder() {
        return SimpleExecutableFinder.forCurrentOperatingSystem(simpleFileFinder());
    }

    public SimpleLocalExecutableFinder simpleLocalExecutableFinder() {
        return new SimpleLocalExecutableFinder(simpleExecutableFinder());
    }

    public SimpleSystemExecutableFinder simpleSystemExecutableFinder() {
        return new SimpleSystemExecutableFinder(simpleExecutableFinder());
    }

    public SimpleExecutableResolver executableResolver() {
        CachedExecutableResolverOptions options = new CachedExecutableResolverOptions();
        options.python3 = false;
        return new SimpleExecutableResolver(options, simpleLocalExecutableFinder(), simpleSystemExecutableFinder());
    }
}
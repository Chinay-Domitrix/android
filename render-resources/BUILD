load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:kotlin.bzl", "kotlin_library", "kotlin_test")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.render-resources",
    srcs = ["src"],
    iml_files = ["intellij.android.render-resources.iml"],
    resources = ["resources"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk",
        "//tools/base/sdklib:studio.android.sdktools.sdklib[module]",
        "//tools/adt/idea/layoutlib-loader:intellij.android.layoutlib-loader[module]",
        "//tools/base/resource-repository:studio.android.sdktools.resource-repository[module]",
        "//tools/base/zipflinger:studio.android.sdktools.zipflinger[module]",
    ],
)

# Build target to verify that render-resources builds for tools/base
kotlin_library(
    name = "tools.render-resources",
    srcs = glob([
        "src/**/*.java",
        "src/**/*.kt",
    ]),
    resources = glob(["resources/**"]),
    visibility = ["//visibility:public"],
    deps = [
        "//prebuilts/tools/common/lint-psi/intellij-core",
        "//tools/adt/idea/layoutlib-loader:tools.layoutlib-loader",
        "//tools/base/common:tools.common",
        "//tools/base/layoutlib-api:tools.layoutlib-api",
        "//tools/base/resource-repository:tools.resource-repository",
        "//tools/base/sdk-common:tools.sdk-common",
        "//tools/base/sdklib:tools.sdklib",
        "//tools/base/zipflinger",
        "@maven//:net.sf.kxml.kxml2",
    ],
)

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.render-resources.tests",
    iml_files = ["intellij.android.render-resources.tests.iml"],
    # keep sorted
    test_data = [
        "//tools/base/resource-repository/test/resources",
    ],
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk[test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/base/resource-repository:studio.android.sdktools.resource-repository[module, test]",
        "//tools/adt/idea/render-resources:intellij.android.render-resources[module, test]",
        "//tools/adt/idea/.idea/libraries:truth[test]",
        "//tools/adt/idea/layoutlib-loader:intellij.android.layoutlib-loader[module, test]",
        "//tools/adt/idea/.idea/libraries:mockito[test]",
    ],
)

# Build target to verify that render-resources tests succeed for tools/base
kotlin_test(
    name = "tools.render-resources.tests",
    srcs = glob([
        "testSrc/**/*.kt",
        "testSrc/**/*.java",
    ]),
    # keep sorted
    data = [
        "//tools/base/resource-repository/test/resources",
    ],
    jvm_flags = ["-Dtest.suite.jar=tools.render-resources.tests.jar"],
    test_class = "com.android.testutils.JarTestSuite",
    deps = [
        ":tools.render-resources",
        "//prebuilts/tools/common/lint-psi/intellij-core",
        "//tools/adt/idea/.idea/libraries:aapt-proto",
        "//tools/adt/idea/layoutlib-loader:tools.layoutlib-loader",
        "//tools/analytics-library/shared:tools.analytics-shared",
        "//tools/base/common:tools.common",
        "//tools/base/layoutlib-api:tools.layoutlib-api",
        "//tools/base/resource-repository:tools.resource-repository",
        "//tools/base/resource-repository:tools.resource-repository.test.util",
        "//tools/base/sdk-common:tools.sdk-common",
        "//tools/base/testutils:tools.testutils",
        "@maven//:com.google.truth.truth",
        "@maven//:junit.junit",
        "@maven//:net.sf.kxml.kxml2",
    ],
)

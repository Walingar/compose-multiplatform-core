/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.metalava

import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class UpdateApiLintBaselineTask @Inject constructor(
    workerExecutor: WorkerExecutor
) : MetalavaTask(workerExecutor) {
    init {
        group = "API"
        description = "Updates an API lint baseline file (api/api_lint.ignore) to match the " +
            "current set of violations. Only use a baseline " +
            "if you are in a library without Android dependencies, or when enabling a new " +
            "lint check, and it is prohibitively expensive / not possible to fix the errors " +
            "generated by enabling this lint check. "
    }

    @get:Input
    abstract val baselines: Property<ApiBaselinesLocation>

    @get:Input
    abstract val targetsJavaConsumers: Property<Boolean>

    @OutputFile
    fun getApiLintBaseline(): File = baselines.get().apiLintFile

    @TaskAction
    fun updateBaseline() {
        check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }
        val baselineFile = baselines.get().apiLintFile
        val checkArgs = getGenerateApiArgs(
            bootClasspath, dependencyClasspath,
            sourcePaths.files.filter { it.exists() }, null, GenerateApiMode.ExperimentalApi,
            ApiLintMode.CheckBaseline(baselineFile, targetsJavaConsumers.get()),
            manifestPath.orNull?.asFile?.absolutePath
        )
        val args = checkArgs + getCommonBaselineUpdateArgs(baselineFile)

        runWithArgs(args)
    }
}

abstract class IgnoreApiChangesTask @Inject constructor(
    workerExecutor: WorkerExecutor
) : MetalavaTask(workerExecutor) {
    init {
        description = "Updates an API tracking baseline file (api/X.Y.Z.ignore) to match the " +
            "current set of violations"
    }

    // The API that the library is supposed to be compatible with
    @get:Input
    abstract val referenceApi: Property<ApiLocation>

    @get:Input
    abstract val api: Property<ApiLocation>

    // The baseline files (api/*.*.*.ignore) to update
    @get:Input
    abstract val baselines: Property<ApiBaselinesLocation>

    @InputFiles
    fun getTaskInputs(): List<File> {
        val referenceApiLocation = referenceApi.get()
        return listOf(
            referenceApiLocation.publicApiFile,
            referenceApiLocation.restrictedApiFile
        )
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        val apiBaselinesLocation = baselines.get()
        return listOf(
            apiBaselinesLocation.publicApiFile,
            apiBaselinesLocation.restrictedApiFile
        )
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }

        updateBaseline(
            api.get().publicApiFile,
            referenceApi.get().publicApiFile,
            baselines.get().publicApiFile,
            false
        )
        if (referenceApi.get().restrictedApiFile.exists()) {
            updateBaseline(
                api.get().restrictedApiFile,
                referenceApi.get().restrictedApiFile,
                baselines.get().restrictedApiFile,
                true
            )
        }
    }

    // Updates the contents of baselineFile to specify an exception for every API present in apiFile but not
    // present in the current source path
    private fun updateBaseline(
        api: File,
        prevApi: File,
        baselineFile: File,
        processRestrictedApis: Boolean
    ) {
        val args = getCommonBaselineUpdateArgs(
            bootClasspath,
            dependencyClasspath,
            baselineFile
        )
        args += listOf(
            "--baseline",
            baselineFile.toString(),
            "--check-compatibility:api:released",
            prevApi.toString(),
            "--source-files",
            api.toString()
        )
        if (processRestrictedApis) {
            args += listOf(
                "--show-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP)",
                "--show-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP_PREFIX)",
                "--show-unannotated"
            )
        }
        runWithArgs(args)
    }
}

private fun getCommonBaselineUpdateArgs(
    bootClasspath: FileCollection,
    dependencyClasspath: FileCollection,
    baselineFile: File
): MutableList<String> {
    val args = mutableListOf(
        "--classpath",
        (bootClasspath.files + dependencyClasspath.files).joinToString(File.pathSeparator)
    )
    args += getCommonBaselineUpdateArgs(baselineFile)
    return args
}

private fun getCommonBaselineUpdateArgs(baselineFile: File): List<String> {
    // Create the baseline file if it does exist, as Metalava cannot handle non-existent files.
    baselineFile.createNewFile()
    return mutableListOf(
        "--update-baseline",
        baselineFile.toString(),
        "--pass-baseline-updates",
        "--delete-empty-baselines",
        "--format=v4"
    )
}

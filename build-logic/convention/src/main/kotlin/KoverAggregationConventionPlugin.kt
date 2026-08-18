import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.CRIOLLO_KOVER_MERGED_MODULE_PATHS
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.CRIOLLO_KOVER_PUBLISHED_LIBRARY_MODULES
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.KoverExcludes.applyLibraryExcludes
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverAggregationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "convention.kover.aggregation must be applied to the root project"
        }

        target.pluginManager.apply(KoverGradlePlugin::class.java)

        target.extensions.configure<KoverProjectExtension> {
            merge {
                subprojects {
                    it.path in CRIOLLO_KOVER_MERGED_MODULE_PATHS
                }
            }
            reports {
                filters {
                    excludes {
                        applyLibraryExcludes()
                    }
                }
                total {
                    filters {
                        excludes {
                            applyLibraryExcludes()
                        }
                        includes {
                            projects.addAll(CRIOLLO_KOVER_PUBLISHED_LIBRARY_MODULES)
                        }
                    }
                    verify {
                        rule("published-library-line-coverage") {
                            minBound(70)
                        }
                    }
                }
            }
        }
    }
}

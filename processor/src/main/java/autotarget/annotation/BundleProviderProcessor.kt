package autotarget.annotation

import autotarget.ProcessorUtil
import autotarget.ProcessorUtil.classBundle
import autotarget.ProcessorUtil.classNonNull
import com.squareup.javapoet.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

abstract class BundleProviderProcessor {

    fun process(processingEnv: ProcessingEnvironment, bundleClasses: HashMap<String, ClassName>) {
        createClasses(processingEnv, bundleClasses)
    }

    private fun createClasses(processingEnv: ProcessingEnvironment, bundleClasses: HashMap<String, ClassName>) {

        val fileBuilder = TypeSpec.classBuilder(getBundlesClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        bundleClasses.forEach { (className, modelClassName) ->
            fileBuilder.addMethod(MethodSpec.methodBuilder("get${className}")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addAnnotation(classNonNull)
                    .returns(modelClassName)
                    .addParameter(ParameterSpec.builder(classBundle, "bundle")
                            .addAnnotation(classNonNull)
                            .build())
                    .addStatement("return new $modelClassName(bundle)")
                    .build())
        }

        val file = fileBuilder.build()
        JavaFile.builder(ProcessorUtil.libraryGeneratedPackageName, file)
                .build()
                .writeTo(processingEnv.filer)
    }

    abstract fun getBundlesClassName(): String

}

package autotarget.annotation

import autotarget.ProcessorUtil
import autotarget.ProcessorUtil.classArrayList
import autotarget.ProcessorUtil.classFragmentTarget
import autotarget.ProcessorUtil.classNonNull
import autotarget.ProcessorUtil.populateParamListBody
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FragmentTargetProcessor {

    private val fragmentsWithPackage: HashMap<String, String> = HashMap()
    private var targetParameterMap: HashMap<String, Element> = HashMap()
    private var fragmentAnnotationMap: HashMap<String, Element> = HashMap()

    fun process(processingEnv: ProcessingEnvironment,
                roundEnv: RoundEnvironment,
                targetParameterMap: HashMap<String, Element>) {

        this.targetParameterMap = targetParameterMap

        val fileBuilder = TypeSpec.classBuilder("FragmentTargets")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        preparePackageMap(processingEnv, roundEnv)
        createMethods(processingEnv, fileBuilder)

        val file = fileBuilder.build()
        JavaFile.builder(ProcessorUtil.libraryGeneratedPackageName, file)
                .build()
                .writeTo(processingEnv.filer)
    }

    private fun preparePackageMap(processingEnv: ProcessingEnvironment, roundEnv: RoundEnvironment) {
        for (it in roundEnv.getElementsAnnotatedWith(FragmentTarget::class.java)) {
            if (!it.kind.isClass) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                        "Can only be applied to a class. Error for ${it.simpleName}")
                continue
            }

            val typeElement = it as TypeElement
            fragmentsWithPackage[typeElement.simpleName.toString()] =
                    processingEnv.elementUtils.getPackageOf(typeElement).qualifiedName.toString()
            fragmentAnnotationMap[typeElement.simpleName.toString()] = it
        }
    }

    private fun createMethods(processingEnv: ProcessingEnvironment, fileBuilder: TypeSpec.Builder) {
        for ((fragmentName, packageName) in fragmentsWithPackage) {
            val annotationElement: Element = fragmentAnnotationMap[fragmentName] ?: continue
            val containerId = annotationElement.getAnnotation(FragmentTarget::class.java).containerId
            val tag = annotationElement.getAnnotation(FragmentTarget::class.java).tag
            val state = annotationElement.getAnnotation(FragmentTarget::class.java).state

            val enterAnimation = annotationElement.getAnnotation(FragmentTarget::class.java).enterAnim
            val exitAnimation = annotationElement.getAnnotation(FragmentTarget::class.java).exitAnim
            val popEnterAnimation = annotationElement.getAnnotation(FragmentTarget::class.java).popEnterAnim
            val popExitAnimation = annotationElement.getAnnotation(FragmentTarget::class.java).popExitAnim

            val fragmentClass = ClassName.get(packageName, fragmentName)
            val parameterMap = ProcessorUtil.createTargetParameterMap(annotationElement)

            parameterMap.keys.forEach {
                val forDefaultGroup = it == ProcessorUtil.libraryDefaultGroupKey
                        || it == ProcessorUtil.libraryOptionalGroupKey
                val parameterItems = parameterMap[it] ?: ArrayList()

                val methodName = if (forDefaultGroup) "show${fragmentName}"
                else "show${fragmentName}For${it.toLowerCase(Locale.ROOT).capitalize()}"

                val methodBuilder = MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addAnnotation(classNonNull)
                        .returns(classFragmentTarget)
                        .addStatement("${ProcessorUtil.listOfParameterProvider} parameterList = new $classArrayList<>()")

                populateParamListBody(processingEnv, parameterItems, methodBuilder)
                methodBuilder.addStatement("return new $classFragmentTarget(" +
                        "new $fragmentClass(), $containerId, \"$tag\", $state, $enterAnimation, " +
                        "$exitAnimation, $popEnterAnimation, $popExitAnimation, parameterList)")
                fileBuilder.addMethod(methodBuilder.build())
            }
        }
    }
}

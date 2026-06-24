#!/bin/bash

# List of all 82 JARs to verify
JARS=(
    "org/jetbrains/kotlin/kotlin-stdlib-jdk8/1.8.20-RC2/kotlin-stdlib-jdk8-1.8.20-RC2.jar"
    "org/apache/httpcomponents/httpmime/4.5.6/httpmime-4.5.6.jar"
    "commons-io/commons-io/2.4/commons-io-2.4.jar"
    "org/ow2/asm/asm-commons/9.2/asm-commons-9.2.jar"
    "org/ow2/asm/asm-util/9.2/asm-util-9.2.jar"
    "org/ow2/asm/asm-analysis/9.2/asm-analysis-9.2.jar"
    "org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
    "org/ow2/asm/asm/9.2/asm-9.2.jar"
    "org/bouncycastle/bcpkix-jdk15on/1.67/bcpkix-jdk15on-1.67.jar"
    "org/bouncycastle/bcprov-jdk15on/1.67/bcprov-jdk15on-1.67.jar"
    "org/glassfish/jaxb/jaxb-runtime/2.3.2/jaxb-runtime-2.3.2.jar"
    "org/glassfish/jaxb/txw2/2.3.2/txw2-2.3.2.jar"
    "org/jvnet/staxex/stax-ex/1.8.1/stax-ex-1.8.1.jar"
    "com/sun/istack/istack-commons-runtime/3.0.8/istack-commons-runtime-3.0.8.jar"
    "com/sun/xml/fastinfoset/FastInfoset/1.2.16/FastInfoset-1.2.16.jar"
    "jakarta/xml/bind/jakarta.xml.bind-api/2.3.2/jakarta.xml.bind-api-2.3.2.jar"
    "jakarta/activation/jakarta.activation-api/1.2.1/jakarta.activation-api-1.2.1.jar"
    "javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar"
    "javax/inject/javax.inject/1/javax.inject-1.jar"
    "com/google/dagger/dagger/2.28.3/dagger-2.28.3.jar"
    "com/google/jimfs/jimfs/1.1/jimfs-1.1.jar"
    "com/squareup/javapoet/1.10.0/javapoet-1.10.0.jar"
    "com/squareup/javawriter/2.5.0/javawriter-2.5.0.jar"
    "net/sf/jopt-simple/jopt-simple/4.9/jopt-simple-4.9.jar"
    "com/googlecode/juniversalchardet/juniversalchardet/1.0.3/juniversalchardet-1.0.3.jar"
    "org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar"
    "net/sf/kxml/kxml2/2.3.0/kxml2-2.3.0.jar"
    "xerces/xercesImpl/2.12.0/xercesImpl-2.12.0.jar"
    "com/sun/activation/javax.activation/1.2.0/javax.activation-1.2.0.jar"
    "org/jetbrains/kotlin/kotlin-stdlib/1.8.20-RC2/kotlin-stdlib-1.8.20-RC2.jar"
    "org/jetbrains/kotlin/kotlin-stdlib-jdk7/1.8.20-RC2/kotlin-stdlib-jdk7-1.8.20-RC2.jar"
    "org/jetbrains/kotlin/kotlin-reflect/1.8.20-RC2/kotlin-reflect-1.8.20-RC2.jar"
    "org/jetbrains/kotlin/kotlin-stdlib-common/1.8.20-RC2/kotlin-stdlib-common-1.8.20-RC2.jar"
    "org/jetbrains/annotations/13.0/annotations-13.0.jar"
    "org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar"
    "io/grpc/grpc-api/1.45.1/grpc-api-1.45.1.jar"
    "io/grpc/grpc-core/1.45.1/grpc-core-1.45.1.jar"
    "io/grpc/grpc-netty/1.45.1/grpc-netty-1.45.1.jar"
    "io/grpc/grpc-protobuf/1.45.1/grpc-protobuf-1.45.1.jar"
    "io/grpc/grpc-protobuf-lite/1.45.1/grpc-protobuf-lite-1.45.1.jar"
    "io/grpc/grpc-stub/1.45.1/grpc-stub-1.45.1.jar"
    "io/grpc/grpc-context/1.45.1/grpc-context-1.45.1.jar"
    "com/google/protobuf/protobuf-java/3.19.3/protobuf-java-3.19.3.jar"
    "com/google/protobuf/protobuf-java-util/3.19.3/protobuf-java-util-3.19.3.jar"
    "com/google/api/grpc/proto-google-common-protos/2.0.1/proto-google-common-protos-2.0.1.jar"
    "com/google/crypto/tink/tink/1.7.0/tink-1.7.0.jar"
    "com/google/code/gson/gson/2.8.9/gson-2.8.9.jar"
    "com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0.jar"
    "org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar"
    "org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar"
    "commons-codec/commons-codec/1.11/commons-codec-1.11.jar"
    "org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar"
    "com/google/guava/guava/31.1-jre/guava-31.1-jre.jar"
    "com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar"
    "com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"
    "org/checkerframework/checker-qual/3.12.0/checker-qual-3.12.0.jar"
    "com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar"
    "com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"
    "org/codehaus/mojo/animal-sniffer-annotations/1.19/animal-sniffer-annotations-1.19.jar"
    "com/google/errorprone/error_prone_annotations/2.11.0/error_prone_annotations-2.11.0.jar"
    "com/google/auto/value/auto-value-annotations/1.6.2/auto-value-annotations-1.6.2.jar"
    "io/perfmark/perfmark-api/0.23.0/perfmark-api-0.23.0.jar"
    "io/netty/netty-handler/4.1.72.Final/netty-handler-4.1.72.Final.jar"
    "io/netty/netty-codec-socks/4.1.72.Final/netty-codec-socks-4.1.72.Final.jar"
    "io/netty/netty-codec/4.1.72.Final/netty-codec-4.1.72.Final.jar"
    "io/netty/netty-transport/4.1.72.Final/netty-transport-4.1.72.Final.jar"
    "io/netty/netty-buffer/4.1.72.Final/netty-buffer-4.1.72.Final.jar"
    "io/netty/netty-resolver/4.1.72.Final/netty-resolver-4.1.72.Final.jar"
    "io/netty/netty-common/4.1.72.Final/netty-common-4.1.72.Final.jar"
    "io/netty/netty-codec-http/4.1.72.Final/netty-codec-http-4.1.72.Final.jar"
    "io/netty/netty-codec-http2/4.1.72.Final/netty-codec-http2-4.1.72.Final.jar"
    "io/netty/netty-handler-proxy/4.1.72.Final/netty-handler-proxy-4.1.72.Final.jar"
    "net/java/dev/jna/jna/5.6.0/jna-5.6.0.jar"
    "net/java/dev/jna/jna-platform/5.6.0/jna-platform-5.6.0.jar"
    "org/bouncycastle/bcprov-jdk15to18/1.72/bcprov-jdk15to18-1.72.jar"
    "org/bouncycastle/bcpkix-jdk15to18/1.72/bcpkix-jdk15to18-1.72.jar"
    "org/bouncycastle/bcutil-jdk15to18/1.72/bcutil-jdk15to18-1.72.jar"
    "org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar"
    "org/bitbucket/b_c/jose4j/0.7.0/jose4j-0.7.0.jar"
    "xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar"
    "commons-logging/commons-logging/1.2/commons-logging-1.2.jar"
    "com/google/android/annotations/4.1.1.4/annotations-4.1.1.4.jar"
)

MISSING=0
ZERO_SIZE=0
TOTAL=${#JARS[@]}

echo "Verifying ${TOTAL} JAR files..."
echo "================================"

for jar in "${JARS[@]}"; do
    if [ ! -f "$jar" ]; then
        echo "MISSING: $jar"
        MISSING=$((MISSING + 1))
    else
        SIZE=$(stat -c%s "$jar" 2>/dev/null || stat -f%z "$jar" 2>/dev/null)
        if [ "$SIZE" -eq 0 ]; then
            echo "ZERO SIZE: $jar"
            ZERO_SIZE=$((ZERO_SIZE + 1))
        else
            echo "OK ($SIZE bytes): $jar"
        fi
    fi
done

echo "================================"
echo "Total: ${TOTAL}"
echo "Missing: ${MISSING}"
echo "Zero size: ${ZERO_SIZE}"
echo "Valid: $((TOTAL - MISSING - ZERO_SIZE))"

if [ $MISSING -gt 0 ] || [ $ZERO_SIZE -gt 0 ]; then
    exit 1
fi

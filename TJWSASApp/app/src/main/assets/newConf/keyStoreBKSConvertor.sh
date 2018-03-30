clear
echo
keytool \
      -import \
      -v \
      -trustcacerts \
      -alias 0 \
      -file <(openssl x509 -in tjws.pem) \
      -keystore tjws.bks \
      -storetype BKS \
      -provider org.bouncycastle.jce.provider.BouncyCastleProvider \
      -providerpath ../../../../libs/bcprov-jdk15on-159.jar \
      -storepass password

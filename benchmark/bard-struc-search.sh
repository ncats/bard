#!/bin/sh

top=500
skip=1
base="http://bard.nih.gov/api/v10"
if (("$#" > 0)); then
  base=$1
fi
url="$base/compounds?expand=true"
echo "Searching against $base..."
queries=("C\\[S@%2b\\](\\[O-\\])C1=CC=CC=C1" "C1=CC=NC=C1" "C1CCCCC1" "C1=CN=CN=C1" "N1C=CC=C1" "C1CCNCC1" "N1C=CN=C1" "N1C=CC2=C1C=CC=C2" "C1CCNC1" "C1=CC2=C(C=C1)N=CC=C2" "C1CNCCN1" "N1C=CC=N1" "C1=CC=C(C=C1)C2=CC=CC=C2" "S1C=CC=C1" "C(C1=CC=CC=C1)C2=CC=CC=C2" "C1=CC2=C(C=C1)C=CC=C2" "C1CCCC1" "S1C=CN=C1" "C(NC1=CC=CC=C1)C2=CC=CC=C2" "O1C=CC=C1" "C1CCOCC1" "C1CCOC1" "C1CCC2CCCCC2C1" "C1CC1" "N1C=NC2=C1C=CC=C2" "C1COCCN1" "C1CCC=CC1" "C1=CC2=C(C=C1)N=CN=C2" "C(OC1=CC=CC=C1)C2=CC=CC=C2" "O=C(NC1=CC=CC=C1)C2=CC=CC=C2" "C1=CN=CC=N1" "N1C=NN=C1" "C1CC2CCCC(C1)C2" "C1CN(CCN1)C2=CC=CC=C2" "O(C1=CC=CC=C1)C2=CC=CC=C2" "C1CC2=C(N1)C=CC=C2" "O=C1NC=CC=C1" "C(N1CCNCC1)C2=CC=CC=C2" "N1C=CC(=N1)C2=CC=CC=C2" "C1OC2=C(O1)C=CC=C2" "C1CCC(CC1)C2CCCCC2" "O=C1C=CNC=N1" "N(C1=CC=CC=C1)C2=NC=NC=C2" "C1CC2CCCCC2C1" "C(NCC1=CC=CC=C1)C2=CC=CC=C2" "C1=CC=C(C=C1)C2=NC=CC=C2" "C1CCC2=C(C1)C=CC=C2" "C(N1CCCCC1)C2=CC=CC=C2" "O1C=CN=C1" "C1CC(CCN1)C2=CC=CC=C2" "N1C=NC2=C1C=NC=N2" "C(N1C=CN=C1)C2=CC=CC=C2" "C1CC2=C(CN1)C=CC=C2" "N1C=NC(=C1)C2=CC=CC=C2" "C1=CN(N=C1)C2=CC=CC=C2" "C1=CN=NC=C1" "C1CCC(CC1)C2=CC=CC=C2" "N1C=NN=N1" "C(CC1=CC=CC=C1)NCC2=CC=CC=C2" "C(CC1=CC=CC=C1)C2=CC=CC=C2" "O=C1C=COC2=C1C=CC=C2" "C1=CC=C(C=C1)C2=CN=CC=C2" "C1CSCN1" "C1NCC2=C1C=CC=C2" "C1COC2=C(C1)C=CC=C2" "O=C1C=COC=C1" "O1C=CC=N1" "C1CCC2C(C1)CCC3CCCCC23" "C1CC2=C(C1)C=CC=C2" "O=C1NC=CC(=O)N1" "C(C=CC1=CC=CC=C1)C2=CC=CC=C2" "C(CC1=CC=CC=C1)CC2=CC=CC=C2" "O=C1OC=CC=C1" "C1CCC(CC1)C2CCC3CCCCC3C2" "S1C=NC2=C1C=CC=C2" "O1C=CC2=C1C=CC=C2" "N1N=CC2=C1C=CC=C2" "C(N1C=CC=C1)C2=CC=CC=C2" "N(C1=CC=CC=C1)C2=NC=CC=N2" "C1=CC2=C(C=C1)C=NC=C2" "C1CCC(C1)C2CCCCC2" "C1CNCN1" "N1C=CC=C1C2=CC=CC=C2" "C1=CC2=C(C=C1)N=CC=N2" "C1CC2CCC3CCCCC3C2C1" "C(CNCCC1=CC=CC=C1)CC2=CC=CC=C2" "C(NC1=CC=CC=C1)NC2=CC=CC=C2" "N(C1=CC=CC=C1)C2=CC=NC=C2" "O=C1OC2=C(C=CC=C2)C=C1" "N1C=CN=C1C2=CC=CC=C2" "C1CC(CN1)C2=CC=CC=C2" "O=S(=O)(NC1=CC=CC=C1)C2=CC=CC=C2" "C(CC1CCCCC1)C2CCCC2" "O=C1CC2=C(N1)C=CC=C2" "C(N1C=CC2=C1C=CC=C2)C3=CC=CC=C3" "C1CC2CCC(CC2C1)C3CCCCC3" "N(C1=CC=CC=C1)C2=C3C=CC=CC3=NC=C2" "N1C=CN=N1" "C(CC1CCCCC1)C2CCCCC2" "O=C(NC1=CC=CC=C1)NC2=CC=CC=C2" "C(=CC1=CC=CC=C1)C2=CC=CC=C2" "N1C2=C(C=CC=C2)C3=C1C=CC=C3" "C(CN1CCCCC1)CC2=CC=CC=C2" "O=C1CCCO1" "C1=CC2=CC3=C(C=CC=C3)N=C2C=C1" "O=C1CCCC=C1" "C1=CC=C(C=C1)C2=NC=NC=C2" "O1C=CC=C1C2=CC=CC=C2" "C1CCC2=CCCCC2C1" "O=C1NC=NC2=C1C=CC=C2" "O=C1C=CNC=C1" "O=C1C=CNC2=C1C=CC=C2" "C1CC=CCN1" "S1C=CC2=C1C=CC=C2" "C1CCC(C1)C2CCC3CCCCC3C2" "C1CC2CCC3C(CCC4CCCCC34)C2C1" "C1CN=CN1" "N(C1=CC=CC=C1)C2=CC=CC=C2" "C1=CC=C(C=C1)C2=CC=NC=C2" "C1=CN(C=N1)C2=CC=CC=C2" "C1CCC1" "C1CNC1" "O=C(NCCC1=CC=CC=C1)C2=CC=CC=C2" "C(CNCC1=CC=CC=C1)CC2=CC=CC=C2" "O=C(NCC1=CC=CC=C1)C2=CC=CC=C2" "C1C2CC3CC1CC(C2)C3" "O=C1CCCN1" "C(CNCCNCCC1=CC=CC=C1)CC2=CC=CC=C2" "O1C=NC=N1" "C1COC(C1)N2C=CN=C2" "O=C(C=CC1=CC=CC=C1)C2=CC=CC=C2" "O=C1NC(=O)C2=C1C=CC=C2" "C1NCC=C1" "C1CC2=C(O1)C=CC=C2" "C1=CN(C=C1)C2=CC=CC=C2" "C(N1CCCC1)C2=CC=CC=C2" "O=S(=O)(NCC1=CC=CC=C1)C2=CC=CC=C2" "C1CCC2CC=CCC2C1" "S1C=CC=C1C2=CC=CC=C2" "S1C=NC(=C1)C2=CC=CC=C2" "C(CN1CCCC1)CC2=CC=CC=C2" "N1N=NC(=N1)C2=CC=CC=C2" "C1=CC=C(C=C1)C2=NC=CC=N2" "O=C(CCC1=CC=CC=C1)NCCC2=CC=CC=C2" "C1CNC2=C(C1)C=CC=C2" "C1NCC2=C1C=CC3=C2C=CN3" "C1CCCNCC1" "C(NC1=NC=NC=C1)C2=CC=CC=C2" "C(CC1CCC2CCCCC2C1)C3CCCCC3" "C1NCC2=C1C=CC3=C2C4=C(N3)C=CC=C4" "C(CNCCNCCNCCC1=CC=CC=C1)CC2=CC=CC=C2" "O1C=NC2=C1C=CC=C2" "C1=NC=NC=N1" "C1CC2CCCC1N2" "N1C=CC2=C1N=CN=C2" "C1COC(C1)N2C=NC3=C2N=CN=C3" "O=C1C=COC(=C1)C2=CC=CC=C2" "C(C1CCNCC1)C2=CC=CC=C2" "N(C1=CC=CC=C1)C2=C3C=CC=CC3=NC=N2" "C1CC=NN1" "C1CCC2C(C1)CC=C3CCCCC23" "C1C=CCC2=C1C=CC=C2" "S1C=NN=C1" "C1CN(CCN1)C2=NC=CC=C2" "C1=CN2C=CC=CC2=N1" "C1=CC2=C(N=C1)N=CN=C2" "O=C(N1CCNCC1)C2=CC=CC=C2" "C1=NC=NN=C1" "O=C1NN=CC=C1" "C1CCC2=C(C1)C=CC=N2" "O1C=CN=C1C2=CC=CC=C2" "O=C(C1=CC=CC=C1)C2=CC=CC=C2" "C(CC1=CC=CC=C1)NC2=CC=CC=C2" "N1C=CC(=C1)C2=CC=CC=C2" "O=C1CCCCC1" "C1CO1" "O=C1C=C(OC2=C1C=CC=C2)C3=CC=CC=C3" "O1C=NN=C1" "N1N=CC2=C1N=CN=C2" "C(NC1=CC=CC=C1)C=CC2=CC=CC=C2" "C1=CC=C(C=C1)C2=NC=CN=C2" "C(NC1=CC=CC=C1)C2=CN=CC=C2" "N1C=C(C=N1)C2=CC=CC=C2" "C1CC=CC1" "C1OCC=C1" "O=C1CNC(=O)N1" "C1CC1C2=CC=CC=C2" "C1C=CCC=C1" "C(CNC1=CC=CC=C1)CC2=CC=CC=C2" "O=C1C=CC(=O)C2=C1C=CC=C2" "N1C=NC(=N1)C2=CC=CC=C2" "C1NCC(S1)=CC2=CC=CC=C2" "N1C2=C(C=CC=C2)N=C1C3=CC=CC=C3" "C1COC2=C(O1)C=CC=C2" "N1C(=CC2=C1C=CC=C2)C3=CC=CC=C3" "C(CNCCNCCNCCNCCC1=CC=CC=C1)CC2=CC=CC=C2" "C1CN(CCN1)C2=NC=NC=C2" "N1N=NC(=N1)C2=C(C=CC=C2)C3=CC=CC=C3" "O=C1NC2=C(C=CC=C2)C=C1" "O=C1CCN1" "C(CC1=CC=CC=C1)NCC2CCCN2" "O=C1NC(=O)C=C1" "O=C1NC(=O)C2=C1C=CC3=C2C=CN3" "C1=CC=C(C=C1)C2=CN=CN=C2")
for s in ${queries[*]}; do
    /bin/echo -n "$s..."
    status=`curl -w '\n%{http_code} %{size_download}b %{time_total}s' -s "${url}&filter=$s\[structure\]&top=$top&skip=$skip" | tail -1` 
    /bin/echo "$status"
done
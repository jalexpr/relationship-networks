package ru.textanalysis.tawt.rn;

import ru.textanalysis.tawt.ms.model.jmorfsdk.Form;

import java.util.List;
import java.util.Set;

public class ExampleRN {
	public static void main(String[] args) {
		RelationshipNetworks relationshipNetworks = new RelationshipNetworks();
		relationshipNetworks.init();

		Set<List<Form>> row = relationshipNetworks.getWords("копейка");
		for (List<Form> word : row) {
			System.out.println(word);
		}

//[{TF=INITIAL,isInit=true,hash=68415300,str='средство',ToS=17,morf=111}]
//[{TF=INITIAL,isInit=true,hash=77263548,str='червонец',ToS=17,morf=103}]
//[{TF=INITIAL,isInit=true,hash=18064338,str='деньга',ToS=17,morf=2305843009213694059}]
//[{TF=INITIAL,isInit=true,hash=18064888,str='деньжишки',ToS=17,morf=4294967411}]
//[{TF=INITIAL,isInit=true,hash=18065400,str='деньжонки',ToS=17,morf=4294967411}]
//[{TF=INITIAL,isInit=true,hash=18029446,str='денежка',ToS=17,morf=107}]
//[{TF=INITIAL,isInit=true,hash=35227119,str='монета',ToS=17,morf=107}]
//[{TF=INITIAL,isInit=true,hash=75185197,str='финансы',ToS=17,morf=4294967411}]
//[{TF=INITIAL,isInit=true,hash=29705760,str='копейка',ToS=17,morf=107}]
//[{TF=INITIAL,isInit=true,hash=18064458,str='деньжата',ToS=17,morf=4294967411}]
//[{TF=INITIAL,isInit=true,hash=27582448,str='капитал',ToS=17,morf=103}]
	}
}

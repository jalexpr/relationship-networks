package ru.textanalysis.tawt.rn;

import lombok.extern.slf4j.Slf4j;
import ru.textanalysis.tawt.gama.Gama;
import ru.textanalysis.tawt.gama.GamaImpl;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.model.gama.BearingPhrase;
import ru.textanalysis.tawt.ms.model.gama.Sentence;
import ru.textanalysis.tawt.ms.model.gama.Word;
import ru.textanalysis.tawt.ms.model.jmorfsdk.Form;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Main {

	private final static int depth = 0;

	private final static RelationshipNetworks relationshipNetworks = new RelationshipNetworks(depth);
	private final static JMorfSdk jMorfSdk = JMorfSdkFactory.loadFullLibrary();
	private final static Gama gama = new GamaImpl();

	public static void main(String[] args) {
		relationshipNetworks.init();
		gama.init();

		System.out.println("init rows = " + relationshipNetworks.rowsSize());
		System.out.println("init words = " + relationshipNetworks.wordsSize());

		String word = "деньги";
		List<String> text = List.of(
			"Стало ясно, что будет с российской валютой",
			"Стало понятно, собственно что станет с русскими деньгами",
			"Копейка рубль бережет",
			"Монетарная политика пошатнула российскую валюту",
			"Играешь числами – создаешь капитал, играешь словами – вечные ценности",
			"Собственность обязывает и крепко привязывает",
			"Наличность выходит из обихода"
		);

		int initKey = jMorfSdk.getOmoForms(word).get(0).getInitialFormKey();
		relationshipNetworks.getWords(initKey).stream()
			.map(se -> se.get(depth))
			.map(relationshipNetworks::getRows)
			.flatMap(Collection::stream)
			.forEach(myKey -> {
				List<Form> forms = jMorfSdk.getOmoForms(myKey);
				forms.forEach(form -> {
					System.out.println(form.getMyString() + " " + form.getMorphCharacteristics());
				});
			});

		//Поиск предложений, где употребляется синоним слова "Деньги", для изменения уровня, необходимо менять значение depth
		for (int i = 0; i <= depth; i++) {
			printSentences(getKindredSentencesCache(word, text, i));
		}

//        long start;
//        long end;
//        StringBuilder stringBuffer = new StringBuilder();
//        for (int j = 0; j < 3; j++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i <= 100; i++) {
//                getKindredSentencesCache(word, text, j);
//                if (i % 10 == 0) {
//                    end = System.currentTimeMillis();
//                    stringBuffer.append("i ").append(i).append(" depth = ").append(j).append(" time = ").append(end - start).append("\n");
//                }
//            }
//        }
//        System.out.println(stringBuffer);
//        System.out.println();
//        stringBuffer = new StringBuilder();
//        for (int j = 2; j < 3; j++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i <= 100; i++) {
//                getKindredSentencesGraph(word, text, j);
//                if (i % 10 == 0) {
//                    end = System.currentTimeMillis();
//                    stringBuffer.append("i ").append(i).append(" depth = ").append(j).append(" time = ").append(end - start).append("\n");
//                    System.out.println(stringBuffer);
//                }
//            }
//            System.out.println(stringBuffer);
//        }
	}

	private static List<Sentence> getKindredSentencesGraph(String srcWord, List<String> text, int depth) {
		Integer intForm = jMorfSdk.getOmoForms(srcWord).get(0).getInitialFormKey();

		List<List<Integer>> senses = relationshipNetworks.getWords(intForm);
		Set<Integer> srcRowWords = extracted(depth, 0, senses);
		return text.stream()
			.map(gama::getMorphSentence)
			.filter(sentence -> sentence.getBearingPhrases().stream()
				.map(BearingPhrase::getWords)
				.flatMap(Collection::stream)
				.map(Word::getOmoForms)
				.flatMap(Collection::stream)
				.anyMatch(word -> {
						if (relationshipNetworks.containsKeyWords(word.getInitialFormKey())) {
							return srcRowWords.contains(word.getInitialFormKey());
						} else {
							return false;
						}
					}
				)
			)
			.collect(Collectors.toList());
	}

	private static Set<Integer> extracted(int depth, int d, List<List<Integer>> senses) {
		if (depth == d) {
			return senses.stream()
				.flatMap(sense -> sense.stream()
					.map(relationshipNetworks::getRows)
					.flatMap(Collection::stream)
				).collect(Collectors.toSet());
		}
		return senses.stream()
			.flatMap(sense -> sense.stream()
				.map(relationshipNetworks::getRows)
				.flatMap(row -> row.stream()
					.map(relationshipNetworks::getWords)
					.map(sensesI -> extracted(depth, d + 1, sensesI))
					.flatMap(Collection::stream)
				)
			).collect(Collectors.toSet());
	}

	private static List<Sentence> getKindredSentencesCache(String srcWord, List<String> text, int depth) {
		System.out.println();
		System.out.println("depth = " + depth + ":");
		Integer intForm = jMorfSdk.getOmoForms(srcWord).get(0).getInitialFormKey();
		Set<Integer> srcRowWords = relationshipNetworks.getWords(intForm).stream()
			.map(se -> se.get(depth))
			.map(relationshipNetworks::getRows)
			.flatMap(Collection::stream)
			.collect(Collectors.toSet());
		return text.stream()
			.map(gama::getMorphSentence)
			.filter(sentence -> sentence.getBearingPhrases().stream()
				.map(BearingPhrase::getWords)
				.flatMap(Collection::stream)
				.map(Word::getOmoForms)
				.flatMap(Collection::stream)
				.anyMatch(word -> {
						if (relationshipNetworks.containsKeyWords(word.getInitialFormKey())) {
							return srcRowWords.contains(word.getInitialFormKey());
						} else {
							return false;
						}
					}
				)
			)
			.collect(Collectors.toList());
	}

	private static void printSentences(List<Sentence> sentences) {
		sentences.stream()
			.map(sentence ->
				sentence.getBearingPhrases().stream()
					.map(BearingPhrase::getWords)
					.flatMap(Collection::stream)
					.map(w -> w.getOmoForms().get(0).getInitialFormString())
					.collect(Collectors.joining(" "))
			).forEach(System.out::println);
	}

	private static void extracted1(Map<Integer, Set<Integer>> ar, String text, String str) throws IOException {
		File f = new File("Babenko_DictionaryOfSynonyms.txt");
		try (FileOutputStream writer = new FileOutputStream(f)) {
			ar.forEach((key, value) -> {
				try {
					writer.write(ByteBuffer.allocate(4).putInt(key).array());
					writer.write((" : ").getBytes(StandardCharsets.UTF_8));
					value.forEach(v -> {
						try {
							writer.write(ByteBuffer.allocate(4).putInt(v).array());
							writer.write(",".getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					writer.write("\n".getBytes(StandardCharsets.UTF_8));
				} catch (Throwable ex) {
					log.warn(ex.getMessage(), ex);
				}
			});
		}
	}

	private static void extracted1(Map<Integer, List<List<Integer>>> ar, String text) throws IOException {
		File f = new File("C:\\Users\\a.porechny\\Desktop\\\\" + text);
		try (FileOutputStream writer = new FileOutputStream(f)) {
			ar.forEach((key, value) -> {
				try {
					writer.write(ByteBuffer.allocate(4).putInt(key).array());
					writer.write((" : ").getBytes(StandardCharsets.UTF_8));
					value.forEach(v -> {
						try {
							writer.write("[".getBytes(StandardCharsets.UTF_8));
							v.forEach(vi -> {
								try {
									writer.write(ByteBuffer.allocate(4).putInt(vi).array());
									writer.write(",".getBytes(StandardCharsets.UTF_8));
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
							writer.write("],".getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					writer.write("\n".getBytes(StandardCharsets.UTF_8));
				} catch (Throwable ex) {
					log.warn(ex.getMessage(), ex);
				}
			});
		}
	}
}

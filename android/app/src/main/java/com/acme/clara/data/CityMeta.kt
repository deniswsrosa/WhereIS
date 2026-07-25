package com.acme.clara.data

/**
 * Per-city geography used for destination clues and the info panel.
 *
 * `real = true`  -> description transcribed verbatim from an authentic in-game screen
 *                   (CARMEN##.BMP captured playthrough shipped in the archive item).
 * `real = false` -> remake-authored. Either framed to the game's 1990 period (e.g.
 *                   "Soviet Union", "Peking") or, where the original stated facts that
 *                   are now false (populations, defunct countries such as Yugoslavia),
 *                   updated to present-day data. NOT byte-exact original text.
 *
 * The original Enhanced build does not store these passages as extractable strings;
 * they are rendered into the city artwork. See corpus/carmen_corpus.md, "Negative findings".
 */
data class CityInfo(
    val name: String,
    val region: String,
    val landmark: String,
    val description: String,
    val real: Boolean,
    val drawable: String? = null, // resource name if we have the authentic photo
)

object CityMeta {
    val all: Map<String, CityInfo> = listOf(
        CityInfo("Athens", "Europe", "the Parthenon",
            "In ancient times, Athens was a powerful city-state that warred with its neighbor Sparta and made lasting contributions to philosophy, science, drama and art.", true, "city_athens"),
        CityInfo("Baghdad", "the Middle East", "the Tigris River",
            "Baghdad, with a population of about 8 million, is Iraq's capital and largest city.", false, "city_baghdad"),
        CityInfo("Bamako", "Africa", "the Niger River",
            "Bamako, with a population of around 3 million, is by far the largest city in Mali, an arid country located in West Africa and extending into the Sahara Desert.", false, "city_bamako"),
        CityInfo("Bangkok", "Asia", "temples called wats",
            "Bangkok, the capital of Thailand, is a bustling city laced with canals and dotted with temples called wats.", true, "city_bangkok"),
        CityInfo("Budapest", "Europe", "the Danube",
            "Hungary, with an area slightly smaller than Indiana, is bordered by Slovakia, Austria, Slovenia, Croatia, Serbia, Romania and Ukraine.", false, "city_budapest"),
        CityInfo("Buenos Aires", "South America", "the Pampas",
            "Argentina is South America's second-largest nation, after Brazil. Its terrain ranges from tropical forests in the north to cold and barren Tierra del Fuego in the south.", true, "city_buenos_aires"),
        CityInfo("Cairo", "Africa", "the Pyramids",
            "Cairo, located at the mouth of the Nile River, is the largest city in Africa.", true, "city_cairo"),
        CityInfo("Colombo", "Asia", "the Indian Ocean coast",
            "Sri Lanka has a hot, humid climate. The principal crops grown on this island nation are tea, coconuts and rice.", true, "city_colombo"),
        CityInfo("Istanbul", "Europe", "the Bosporus",
            "Turkey, which has hot, dry summers and cold winters, is ringed by high mountains on all but the western border.", true, "city_istanbul"),
        CityInfo("Kathmandu", "Asia", "the Himalayas",
            "Nepal was closed to the outside world for centuries, but can now be reached by plane and road from India, Pakistan and Tibet.", true, "city_kathmandu"),
        CityInfo("Kigali", "Africa", "mountain gorillas",
            "Kigali is the capital of Rwanda, a country of lush jungle terrain which is the home of the endangered mountain gorilla.", true, "city_kigali"),
        CityInfo("Lima", "South America", "the Pacific coast",
            "Peru, once the center of the mighty Incan Empire, is a rugged land dominated by the Andes Mountains. Forests and jungles cover half its land area.", true, "city_lima"),
        CityInfo("London", "Europe", "Big Ben",
            "London is the capital of the United Kingdom, which consists of England, Scotland, Wales and Northern Ireland.", true, "city_london"),
        CityInfo("Mexico City", "North America", "the Zocalo",
            "Mexico is about three times the size of Texas, and has terrain ranging from rugged mountains to harsh deserts and tropical lowlands.", true, "city_mexico_city"),
        CityInfo("Montreal", "North America", "the St. Lawrence River",
            "Montreal is the second largest city in Canada. A famous landmark is the church of Notre Dame de Bonsecours.", true, "city_montreal"),
        CityInfo("Moroni", "Africa", "the Indian Ocean",
            "The chief industry of the Comoros is perfume, made from plants grown on the islands.", true, "city_moroni"),
        CityInfo("Moscow", "Europe", "Red Square",
            "The Soviet Union is the largest country in the world. It occupies one-sixth of the earth's land area.", true, "city_moscow"),
        CityInfo("New Delhi", "Asia", "the Red Fort",
            "New Delhi, with a metropolitan population of more than 30 million, is the capital of India, a nation of over 1.4 billion people.", false, "city_new_delhi"),
        CityInfo("New York", "North America", "the Statue of Liberty",
            "The headquarters of the United Nations, located in New York, adds to the multi-cultural nature of the city.", true, "city_new_york"),
        CityInfo("Oslo", "Europe", "the Oslofjord",
            "Oslo, the capital and largest city in Norway, is a major industrial and maritime center as well as the seat of government.", true, "city_oslo"),
        CityInfo("Paris", "Europe", "the Eiffel Tower",
            "Paris, the capital of France, is a center of art and culture, and home of the world famous Eiffel Tower.", true, "city_paris"),
        CityInfo("Peking", "Asia", "the Great Wall",
            "Peking is the capital of the People's Republic of China. One of this nation's most famous landmarks is the Great Wall.", true, "city_peking"),
        CityInfo("Port Moresby", "Oceania", "volcanic mountains",
            "Port Moresby is the capital of Papua New Guinea, a land of jungles and volcanic mountains.", true, "city_port_moresby"),
        CityInfo("Reykjavik", "Europe", "the Althing",
            "Iceland's governmental assembly, the Althing, is the world's oldest surviving parliament.", true, "city_reykjavik"),
        CityInfo("Rio de Janeiro", "South America", "Christ the Redeemer",
            "Brazil's capital is Brasilia, an entirely new city recently built in the interior of the country.", true, "city_rio_de_janeiro"),
        CityInfo("Rome", "Europe", "the Colosseum",
            "During the Renaissance, Italy was divided into numerous city-states which were vital centers of art, science and learning.", true, "city_rome"),
        CityInfo("San Marino", "Europe", "postage stamps",
            "Postage stamps and tourism are San Marino's largest industries. This tiny country's full name is Most Serene Republic of San Marino.", true, "city_san_marino"),
        CityInfo("Singapore", "Asia", "40 smaller islands",
            "The nation of Singapore, located near the equator in Southeast Asia, consists of one large island plus 40 smaller islands.", true, "city_singapore"),
        CityInfo("Sydney", "Oceania", "the Opera House",
            "An island continent, Australia is nearly as large as the United States but has only about one-twelfth the population.", false, "city_sydney"),
        CityInfo("Tokyo", "Asia", "Tokyo Bay",
            "Tokyo, with a population of about 14 million people, is the capital and largest city in Japan.", false, "city_tokyo"),
    ).associateBy { it.name }

    fun of(name: String): CityInfo = all[name] ?: CityInfo(name, "the world", "the city",
        "$name is one of the great cities of the world.", false)
}

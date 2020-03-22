import java.io.File
import java.io.FileNotFoundException
import java.util.Scanner
import kotlin.random.Random

private const val ARG_IMPORT = "-import"
private const val ARG_EXPORT = "-export"

private const val ACTION_ADD = "add"
private const val ACTION_REMOVE = "remove"
private const val ACTION_IMPORT = "import"
private const val ACTION_EXPORT = "export"
private const val ACTION_ASK = "ask"
private const val ACTION_LOG = "log"
private const val ACTION_HARDEST_CARD = "hardest card"
private const val ACTION_RESET_STATS = "reset stats"
private const val ACTION_EXIT = "exit"
private const val MESSAGE_BYE = "Bye bye!"
private const val MESSAGE_CARD_ADDED = "The pair %s has been added.\n"
private const val MESSAGE_CARD_EXISTS = "The card \"%s\" already exists.\n"
private const val MESSAGE_CARDS_STATISTICS_RESET = "Card statistics has been reset.\n"
private const val MESSAGE_CORRECT_ANSWER = "Correct answer.\n"
private const val MESSAGE_DEFINITION_EXISTS = "The definition \"%s\" already exists.\n"
private const val MESSAGE_HARDEST_CARD = "The hardest card is \"%s\". " +
        "You have %s errors answering them.\n"
private const val MESSAGE_HARDEST_CARDS = "The hardest cards are %s. You have %s errors answering them.\n"
private const val MESSAGE_MENU = "Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):"
private const val MESSAGE_NO_CARDS_WITH_ERRORS = "There are no cards with errors.\n"
private const val MESSAGE_NO_SUCH_CARD = "Can't remove \"%s\": there is no such card.\n"
private const val MESSAGE_PRINT_DEFINITION = "Print the definition of \"%s\":"
private const val MESSAGE_THE_CARD = "The card:"
private const val MESSAGE_THE_CARD_REMOVED = "The card has been removed.\n"
private const val MESSAGE_THE_DEFINITION_OF_THE_CARD = "The definition of the card:"
private const val MESSAGE_TIME_TO_ASK = "How many times to ask?"
private const val MESSAGE_WRONG_ANSWER = "Wrong answer. The correct one is \"%s\".\n"
private const val MESSAGE_WRONG_ANSWER_OF_OTHER_TERM = "Wrong answer. The correct one is \"%s\", " +
        "you've just written the definition of \"%s\".\n"

private const val MESSAGE_CARDS_LOADED = "%d cards have been loaded.\n"
private const val MESSAGE_CARDS_SAVED = "%d cards have been saved.\n"
private const val MESSAGE_FILE_NAME = "File name:"
private const val MESSAGE_FILE_NOT_FOUND = "File not found.\n"
private const val MESSAGE_LOG_IS_SAVED = "The log has been saved.\n"
private const val PATH_TO_CARD_FILES = ""

val LOG = CustomLogger
val cardList = mutableListOf<Card>()
val importExportService = ImportExportService()

fun main(args: Array<String>) {
    val argumentMap = getArgumentMap(args)

    argumentMap[ARG_IMPORT]?.let { importExportService.importCardsOnInit(it) }
    Flashcards().runGame()
    argumentMap[ARG_EXPORT]?.let { importExportService.exportCardsOnExit(it) }
}

private fun getArgumentMap(args: Array<String>): MutableMap<String, String> {
    val argumentMap = mutableMapOf<String, String>()

    for (i in args.indices step 2) {
        argumentMap[args[i]] = args[i + 1]
    }

    return argumentMap
}

private fun MutableList<Card>.getByTerm(term: String): Card? {
    return this.singleOrNull { it.term == term }
}

private fun MutableList<Card>.getByDefinition(definition: String): Card? {
    return this.singleOrNull { it.definition == definition }
}

class Flashcards {

    fun runGame() {
        var action = String()

        while (action != ACTION_EXIT) {
            LOG.logAndPrintlnOutput(MESSAGE_MENU)
            action = LOG.logAndGetInput()

            when (action) {
                ACTION_ADD -> addCard()
                ACTION_REMOVE -> removeCard()
                ACTION_IMPORT -> importExportService.importCards()
                ACTION_EXPORT -> importExportService.exportCards()
                ACTION_ASK -> ask()
                ACTION_LOG -> importExportService.exportLog()
                ACTION_HARDEST_CARD -> printHardestCardTerm()
                ACTION_RESET_STATS -> resetMistakeStats()
            }
        }

        LOG.logAndPrintlnOutput(MESSAGE_BYE)
    }

    private fun addCard() {
        val term = getTermFromInput()
        val definition = if (term == null) null else getDefinitionFromInput()

        if (term != null && definition != null) {
            val card = Card(term, definition)
            cardList.add(card)
            LOG.logAndPrintlnOutput(MESSAGE_CARD_ADDED.format(card.toString()))
        }
    }

    private fun getTermFromInput(): String? {
        LOG.logAndPrintlnOutput(MESSAGE_THE_CARD)
        val term = LOG.logAndGetInput()

        if (cardList.getByTerm(term) != null) {
            LOG.logAndPrintlnOutput(MESSAGE_CARD_EXISTS.format(term))
            return null
        }

        return term
    }

    private fun getDefinitionFromInput(): String? {
        LOG.logAndPrintlnOutput(MESSAGE_THE_DEFINITION_OF_THE_CARD)
        val definition = LOG.logAndGetInput()

        if (cardList.getByDefinition(definition) != null) {
            LOG.logAndPrintlnOutput(MESSAGE_DEFINITION_EXISTS.format(definition))
            return null
        }

        return definition
    }

    private fun removeCard() {
        LOG.logAndPrintlnOutput(MESSAGE_THE_CARD)
        val term = LOG.logAndGetInput()
        val cardToRemove = cardList.getByTerm(term)

        if (cardToRemove == null) {
            LOG.logAndPrintlnOutput(MESSAGE_NO_SUCH_CARD.format(term))
        } else {
            cardList.remove(cardToRemove)
            LOG.logAndPrintlnOutput(MESSAGE_THE_CARD_REMOVED)
        }
    }

    private fun ask() {
        LOG.logAndPrintlnOutput(MESSAGE_TIME_TO_ASK)
        val askCount = LOG.logAndGetInput().toInt()
        var askedCounter = 0

        while ({ askedCounter++ }() != askCount) {
            val randomCard = cardList[Random.nextInt(cardList.size)]

            LOG.logAndPrintlnOutput(MESSAGE_PRINT_DEFINITION.format(randomCard.term))
            val answer = LOG.logAndGetInput()

            if (answer == randomCard.definition) {
                LOG.logAndPrintlnOutput(MESSAGE_CORRECT_ANSWER)
            } else {
                val rightCard = cardList.singleOrNull { it.definition == answer }

                if (rightCard == null) {
                    LOG.logAndPrintlnOutput(MESSAGE_WRONG_ANSWER.format(randomCard.definition))
                } else {
                    LOG.logAndPrintlnOutput(MESSAGE_WRONG_ANSWER_OF_OTHER_TERM.format(randomCard.definition, rightCard.term))
                }
                randomCard.mistakesCount++
            }
        }
    }

    private fun printHardestCardTerm() {
        var maxMistakes = 0

        for (card in cardList) {
            if (card.mistakesCount > maxMistakes)
                maxMistakes = card.mistakesCount
        }

        if (maxMistakes > 0) {
            val mostMistakesCardTermList = cardList.filter { it.mistakesCount == maxMistakes }.map { it.term }

            if (mostMistakesCardTermList.size == 1) {
                LOG.logAndPrintlnOutput(MESSAGE_HARDEST_CARD.format(mostMistakesCardTermList[0], maxMistakes))
            } else {
                val termsAsString = "\"" + mostMistakesCardTermList.joinToString("\", \"") + "\""
                LOG.logAndPrintlnOutput(MESSAGE_HARDEST_CARDS.format(termsAsString, maxMistakes))
            }
        } else {
            LOG.logAndPrintlnOutput(MESSAGE_NO_CARDS_WITH_ERRORS)
        }
    }

    private fun resetMistakeStats() {
        cardList.forEach { it.mistakesCount = 0 }
        LOG.logAndPrintlnOutput(MESSAGE_CARDS_STATISTICS_RESET)
    }
}

data class Card(val term: String, var definition: String) {
    var mistakesCount: Int = 0

    constructor(term: String, definition: String, mistakesCount: Int) : this(term, definition) {
        this.mistakesCount = mistakesCount
    }

    override fun toString(): String {
        return "(\"$term\":\"$definition\")"
    }
}

object CustomLogger {
    private val scanner = Scanner(System.`in`)
    val logList = mutableListOf<String>()

    fun logAndPrintlnOutput(message: String) {
        logList.add(message)
        println(message)
    }

    fun logAndGetInput(): String {
        val text = this.scanner.nextLine()
        logList.add(text)
        return text
    }
}

class ImportExportService {

    fun importCards() {
        val file = getFileFromInput()
        importCardsFromFile(file)
    }

    fun importCardsOnInit(fileName: String) {
        val file = File(PATH_TO_CARD_FILES + fileName)
        importCardsFromFile(file)
    }

    private fun importCardsFromFile(file: File) {
        try {
            val cardDataList = file.readLines()

            for (i in cardDataList.indices step 3) {
                addOrUpdateCard(cardDataList[i], cardDataList[i + 1], cardDataList[i + 2].toInt())
            }

            val cardsCount = cardDataList.size / 3
            LOG.logAndPrintlnOutput(MESSAGE_CARDS_LOADED.format(cardsCount))
        } catch (e: FileNotFoundException) {
            LOG.logAndPrintlnOutput(MESSAGE_FILE_NOT_FOUND)
        }
    }

    private fun addOrUpdateCard(term: String, definition: String, mistakesCount: Int) {
        val cardToUpdate = cardList.getByTerm(term)

        if (cardToUpdate == null) {
            cardList.add(Card(term, definition, mistakesCount))
        } else {
            cardToUpdate.definition = definition
            cardToUpdate.mistakesCount = mistakesCount
        }
    }

    fun exportCards() {
        val file = getFileFromInput()
        val text = getFileTextFromCardList()
        val message = MESSAGE_CARDS_SAVED.format(cardList.size)

        export(file, text, message)
    }

    fun exportCardsOnExit(fileName: String) {
        val file = File(PATH_TO_CARD_FILES + fileName)
        val text = getFileTextFromCardList()
        val message = MESSAGE_CARDS_SAVED.format(cardList.size)

        export(file, text, message)
    }

    fun exportLog() {
        val file = getFileFromInput()
        val text = LOG.logList.joinToString("\n")

        export(file, text, MESSAGE_LOG_IS_SAVED)
    }

    private fun export(file: File, text: String, message: String) {
        if (!file.exists()) {
            file.createNewFile()
        }

        file.writeText(text)
        LOG.logAndPrintlnOutput(message)
    }

    private fun getFileTextFromCardList(): String {
        val cardDataList = mutableListOf<String>()
        cardList.forEach{ cardDataList.add(it.term); cardDataList.add(it.definition); cardDataList.add(it.mistakesCount.toString()) }

        return cardDataList.joinToString("\n")
    }

    private fun getFileFromInput(): File {
        LOG.logAndPrintlnOutput(MESSAGE_FILE_NAME)
        val fileName = LOG.logAndGetInput()

        return File(PATH_TO_CARD_FILES + fileName)
    }
}

package org.rbr.blueant.timesheet.parser

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvToBeanBuilder
import java.time.Duration

@NoArgConstructor
data class TimeEntry(
    @CsvBindByName(column = "Projekt-Nr.") val projectNumber: String,
    @CsvBindByName(column = "Projekt") val project: String,
    @CsvBindByName(column = "Dauer") val duration: String
) {
    fun getProjectNrAndProject(): String {
        return "$projectNumber: $project"
    }
}

fun main(args: Array<String>) {
    val timesheetFilename = args.getOrElse(0) { "example.csv" } 
    val timeEntries: List<TimeEntry> = loadAndParseCSV(timesheetFilename)

    timeEntries
        .filter { timeEntry -> timeEntry.projectNumber.isNotBlank() } //ignore sum row without projectNr
        .groupBy(TimeEntry::getProjectNrAndProject)
        .map { (project, timeEntries) ->
            val durationSum = timeEntries
                .map(TimeEntry::duration)
                .map(::fromHHmm)
                .reduce(Duration::plus)
            Pair(project, durationSum)
        }
        .sortedBy { pair -> pair.first }
        .peek { pair -> println(pair.first + " - " + pair.second.toHHdd()) }
        .map { it.second }
        .reduce(Duration::plus)
        .let { println("\nSum: " + it.toHHdd()) }
}

private fun loadAndParseCSV(filename: String): List<TimeEntry> {
    val timesheetReader = { }.javaClass.classLoader.getResourceAsStream(filename)?.bufferedReader()
    if (timesheetReader == null) {
        println("$filename could not be found/loaded...")
        return emptyList()
    } else {
        println("$filename \n")
    }

    repeat(9) {
        timesheetReader.readLine() //ignore first 9 lines of chunk
    }

    return CsvToBeanBuilder<TimeEntry>(timesheetReader)
        .withType(TimeEntry::class.java)
        .withSeparator(';')
        .build()
        .parse()
}

fun Duration.toHHdd(): String {
    val hours = this.toHours()
    val ddFractionOfHour = this.toMinutesPart() * 100 / 60
    return String.format("%02d,%02d", hours, ddFractionOfHour)
}

fun fromHHmm(durationString: String): Duration {
    val split = durationString.split(":".toRegex())
    if (split.size != 2) {
        println("durationString can not be parsed: $durationString")
        return Duration.ZERO
    }
    return Duration.ofHours(split[0].toLong()).plusMinutes(split[1].toLong())
}

fun <T> List<T>.peek(action: (T) -> Unit): List<T> {
    this.forEach { action(it) }
    return this
}
appender("CONSOLE", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%highlight([%thread] - %level - %msg) %n"
	}
}
root(INFO, ["CONSOLE"])
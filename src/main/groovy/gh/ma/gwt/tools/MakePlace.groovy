package gh.ma.gwt.tools

import groovy.util.logging.Slf4j;
import groovyjarjarcommonscli.CommandLineParser;
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.Options


@Slf4j
class MakePlace {

	def cli
	def opts
	
	MakePlace(args) {
		cli = new CliBuilder();
		
		opts = cli.parse(args)
		if (!opts) {
			 throw new GroovyRuntimeException("Bad usage")
		}
	}
	
	void run() {
	}

	static main(args) {
		new MakePlace(args).run()
	}

}

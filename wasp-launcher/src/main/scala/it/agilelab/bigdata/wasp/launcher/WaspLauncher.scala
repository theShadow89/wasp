package it.agilelab.bigdata.wasp.launcher

import akka.actor.Props
import it.agilelab.bigdata.wasp.WASP_VERSION
import it.agilelab.bigdata.wasp.consumers.batch.BatchMasterGuardian
import it.agilelab.bigdata.wasp.consumers.consumers.ConsumersMasterGuardian
import it.agilelab.bigdata.wasp.consumers.readers.KafkaReader
import it.agilelab.bigdata.wasp.consumers.writers.SparkWriterFactoryDefault
import it.agilelab.bigdata.wasp.core.SystemPipegraphs
import it.agilelab.bigdata.wasp.core.utils.{ActorSystemInjector, ConfigManager, WaspDB}
import it.agilelab.bigdata.wasp.core.WaspSystem._
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.LoggerInjector
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.producers.InternalLogProducerGuardian

trait WaspLauncher extends ActorSystemInjector with LoggerInjector with BSONConversionHelper {
	override def loggerActorProps : Props = Props(new InternalLogProducerGuardian(ConfigBL))

	// ASCII art from http://bigtext.org/?font=smslant&text=Wasp
	val banner = """Welcome to
     _       __
    | |     / /___ _ ____ ___
    | | /| / / __ `/ ___/ __ \
    | |/ |/ / /_/ (__  ) /_/ /
    |__/|__/\__,_/____/ .___/    version %s
                     /_/
								               """.format(WASP_VERSION)

	// TODO write usage information (when command line switches are somewhat definitive)
	val usage = """Usage:
			TODO!
		          """.stripMargin

	def main(args: Array[String]) {
		val options = new WaspOptions(args)

		// handle error, version & help
		if (options.error) {
			val value = options.errorValue
			printErrorAndExit(s"Unrecognized option '$value'.")
		} else if (options.version) {
			printVersionAndExit()
		} else if (options.help) {
			printUsageAndExit()
		}

		// print banner if starting anything
		if (options.startApp || options.startStreamingGuardian|| options.startBatchGuardian) println(banner)

		// initialize stuff if starting anything
		if (options.startApp || options.startStreamingGuardian|| options.startBatchGuardian) {
			initializeWasp()
		}

		// handle startup requests
		if (options.startStreamingGuardian) {
			startStreamingGuardian()
		}
		if (options.startBatchGuardian) {
			startBatchGuardian()
		}
		if (options.startApp) {
			startApp(args)
		}
	}

	def initializeWasp() = {
		// db
		WaspDB.DBInitialization(actorSystem)
		// configs
		ConfigManager.initializeConfigs()
		// workloads
		initializeDefaultWorkloads()
		initializeCustomWorkloads()
	}


	private def printErrorAndExit(message: String): Unit = {
		println(message)
		println("Use --help for usage information.")
		System.exit(0)
	}

	private def printVersionAndExit(): Unit = {
		println(banner)
		println("Use --help for more information.")
		System.exit(0)
	}

	private def printUsageAndExit(): Unit = {
		println(banner)
		println(usage)
		System.exit(0)
	}

	private def startApp(args: Array[String]): Unit = {
		// redirect to play framework entry point
		play.core.server.NettyServer.main(Array.empty[String])
	}

	/* private def startMasterGuardian(options: WaspOptions): Unit = {
		val actorSystem = WaspSystem.actorSystem
		WaspSystem.systemInitialization(actorSystem)
		WaspDB.DBInitialization(actorSystem)
		if(startMaster == "true") {
			masterActor = WaspSystem.actorSystem.actorOf(Props(new MasterGuardian(ConfigBL)))
		}
	} */

	private def startStreamingGuardian(): Unit = {
		actorSystem.actorOf(Props(new ConsumersMasterGuardian(ConfigBL, SparkWriterFactoryDefault, KafkaReader)), ConsumersMasterGuardian.name)
	}

	private def startBatchGuardian(): Unit = {
		actorSystem.actorOf(Props(new BatchMasterGuardian(ConfigBL, SparkWriterFactoryDefault)), BatchMasterGuardian.name)
	}

	/**
		* Initialize DB with default workloads (Raw and Logger pipegraphs)
		*/
	private def initializeDefaultWorkloads(): Unit = {
		println("Default workloads initialization.")
		val db = WaspDB.getDB
		// initialize logger pipegraph
		//db.insertIfNotExists[TopicModel](SystemPipegraphs.loggerTopic)
		//db.insertIfNotExists[IndexModel](SystemPipegraphs.loggerIndex)
		//db.insertIfNotExists[PipegraphModel](SystemPipegraphs.loggerPipegraph)
		//db.insertIfNotExists[ProducerModel](SystemPipegraphs.loggerProducer)
		// initialize raw pipegraph
		//db.insertIfNotExists[TopicModel](SystemPipegraphs.rawTopic)
		//db.insertIfNotExists[IndexModel](SystemPipegraphs.rawIndex)
		//db.insertIfNotExists[PipegraphModel](SystemPipegraphs.rawPipegraph)
	}

	/**
		* Launchers must override this with deployment-specific pipegraph initialization logic;
		* this usually simply means loading the custom pipegraphs into the database.
		*/
	def initializeCustomWorkloads(): Unit
}
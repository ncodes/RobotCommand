RobotCommand is a robots.txt parser wriiten in java.Has support for caching.

How to use:
------------

RobotCommand robotCommand = new RobotCommand();
\nrobotCommand.allow("http://example.com/some/directory", "my-user-agent");

// Get crawl-delay\n
robotCommand.crawlDelay

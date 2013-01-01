RobotCommand is a robots.txt parser with support for caching.

How to use

RobotCommand robotCommand = new RobotCommand();
robotCommand.allow("http://example.com/some/directory", "my-user-agent")
import argparse
import datastore
import lorem
import random
import sys

def story_add(args):
    _story_add(args.num_paragraphs)


def _story_add(num_paragraphs):
    def content(num_paragraphs):
        return "\n\n".join([lorem.paragraph() for _ in range(num_paragraphs)])

    def random_reporter():
        return random.choice([reporter.id for reporter in datastore.get_all_reporters()])

    story = datastore.NewsStory(
        headline=lorem.sentence(),
        content=content(num_paragraphs),
        image=_random_image_uri(),
        reporter=random_reporter(),
    )

    datastore.add_news_story(story)
    print("Added one new lorem-ipsum news story")


def _random_image_uri():
    return "https://picsum.photos/seed/%d/XXX/YYY.jpg" % random.randint(0, 1000)


def stories_clear(args):
    datastore.delete_all_news_stories()
    print("Removed all news stories")


def reporter_add(args):
    _reporter_add(args.name, args.pub_key)


def _reporter_add(name, pub_key):
    reporter = datastore.Reporter(pub_key=pub_key, name=name, image=_random_image_uri())

    datastore.add_reporter(reporter)
    print(f"Added new reporter {name} with pub_key `{pub_key}`")


def reporter_list(args):
    reporters = datastore.get_all_reporters()

    print("%-4s %-20s %s" % ("ID", "Name", "Pub Key"))
    for reporter in reporters:
        print("%04d %-20s %s" % (reporter.id, reporter.name, reporter.pub_key))


def reporters_clear(args):
    datastore.delete_all_reporters()
    print("Removed all reporters")


def clear_and_default(args):
    datastore.delete_all()

    _reporter_add("Rosalind Franklin", datastore.get_reporter_key(1))
    _reporter_add("Charles Darwin", datastore.get_reporter_key(2))
    _reporter_add("Isaac Newton", datastore.get_reporter_key(3))

    for _ in range(20):
        _story_add(10)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(prog='CoverDrop CLI')
    subparsers = parser.add_subparsers()

    parser_story_add = subparsers.add_parser(
        'story_add',
        help='Adds a random news story')
    parser_story_add.add_argument(
        '--num-paragraphs',
        type=int, default=5, metavar='n',
        help='Number of paragraphs to create (default: 5)')
    parser_story_add.set_defaults(func=story_add)

    parser_stories_clear = subparsers.add_parser(
        'stories_clear',
        help='Removes all news stories')
    parser_stories_clear.set_defaults(func=stories_clear)

    parser_reporter_add = subparsers.add_parser(
        'reporter_add',
        help='Adds a new reporter with a given name and public key')
    parser_reporter_add.add_argument(
        'pub_key',
        help='Public key in base64.')
    parser_reporter_add.add_argument(
        'name',
        help='Name of the reporter e.g. Rosalind Franklin')
    parser_reporter_add.set_defaults(func=reporter_add)

    parser_reporter_list = subparsers.add_parser(
        'reporter_list',
        help='Lists all reporters')
    parser_reporter_list.set_defaults(func=reporter_list)

    parser_reporters_clear = subparsers.add_parser(
        'reporters_clear',
        help='Removes all reporters')
    parser_reporters_clear.set_defaults(func=reporters_clear)

    parser_clear_and_default = subparsers.add_parser(
        'clear_and_default',
        help='Clears the entire DB and generates a scenario with default reporters and articles')
    parser_clear_and_default.set_defaults(func=clear_and_default)

    args = parser.parse_args()
    if 'func' in args:
        args.func(args)
    else:
        parser.print_help()

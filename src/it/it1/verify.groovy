file = new File(basedir, 'build.log')
assert file

text = file.text
assert text.contains('encoding-detector-maven-plugin')

return true
#! /usr/bin/env nix-shell
`
#! nix-shell -i node -p nodejs-12_x
`

const fs = require('fs')

const filePath = process.argv[2]

content = fs.readFileSync(filePath, 'utf-8')
splittedLines = content.split('\n').map(line => line.split('\t'))

datarefs = splittedLines.map(([name, type, _writable, _unit, description]) => ({ name, type, description }))

console.log(JSON.stringify({ datarefs }, null, 4))

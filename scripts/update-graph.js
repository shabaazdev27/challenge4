const fs = require('fs');
const path = require('path');

const graphPath = path.join(__dirname, '../backend/src/main/resources/data/metlife-graph.json');
const data = JSON.parse(fs.readFileSync(graphPath, 'utf8'));

data.forEach(node => {
  // Add spawn points for gamification
  if (node.type === 'food' || node.type === 'merchandise') {
    node.spawnPoints = ['mascot'];
  } else if (node.type === 'gate') {
    node.spawnPoints = ['coin'];
  }

  // Update edges for weather routing
  if (node.edges) {
    node.edges.forEach(edge => {
      // Gates, ramps and sections are considered open-air/uncovered for demo purposes.
      if (edge.to.startsWith('GATE') || edge.to.startsWith('RAMP') || edge.to.startsWith('SEC') || node.nodeId.startsWith('GATE') || node.nodeId.startsWith('RAMP') || node.nodeId.startsWith('SEC')) {
        edge.isCovered = false;
      } else {
        edge.isCovered = true;
      }
    });
  }
});

fs.writeFileSync(graphPath, JSON.stringify(data, null, 2), 'utf8');
console.log('Graph updated successfully.');

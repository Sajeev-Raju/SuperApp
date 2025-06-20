export { matchers } from './matchers.js';

export const nodes = [
	() => import('./nodes/0'),
	() => import('./nodes/1'),
	() => import('./nodes/2'),
	() => import('./nodes/3'),
	() => import('./nodes/4'),
	() => import('./nodes/5'),
	() => import('./nodes/6'),
	() => import('./nodes/7'),
	() => import('./nodes/8'),
	() => import('./nodes/9'),
	() => import('./nodes/10'),
	() => import('./nodes/11'),
	() => import('./nodes/12'),
	() => import('./nodes/13'),
	() => import('./nodes/14'),
	() => import('./nodes/15'),
	() => import('./nodes/16'),
	() => import('./nodes/17'),
	() => import('./nodes/18'),
	() => import('./nodes/19'),
	() => import('./nodes/20'),
	() => import('./nodes/21'),
	() => import('./nodes/22')
];

export const server_loads = [];

export const dictionary = {
		"/": [2],
		"/business": [3],
		"/business/create": [5],
		"/business/[id]": [4],
		"/classifieds": [6],
		"/classifieds/create": [8],
		"/classifieds/[id]": [7],
		"/dashboard": [9],
		"/emergency": [10],
		"/meetups": [11],
		"/meetups/create": [13],
		"/meetups/my-events": [14],
		"/meetups/[id]": [12],
		"/polls": [15],
		"/polls/create": [17],
		"/polls/[collectionId]": [16],
		"/profile": [18],
		"/questions": [19],
		"/questions/create": [21],
		"/questions/[id]": [20],
		"/setup": [22]
	};

export const hooks = {
	handleError: (({ error }) => { console.error(error) }),
	
	reroute: (() => {}),
	transport: {}
};

export const decoders = Object.fromEntries(Object.entries(hooks.transport).map(([k, v]) => [k, v.decode]));

export const hash = false;

export const decode = (type, value) => decoders[type](value);

export { default as root } from '../root.svelte';
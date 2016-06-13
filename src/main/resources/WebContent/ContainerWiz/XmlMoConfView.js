Ember.Select.reopen({
	 _changeSingle: function() {
		var el = this.$()[0],
			selectedIndex = el && el.selectedIndex,
			content = Ember.get(this, 'content'),
			prompt = Ember.get(this, 'prompt');

		if (!content) { return; }
		if (prompt && selectedIndex === 0) { Ember.set(this, 'selection', null); return; }

		if (prompt) { selectedIndex -= 1; }
		Ember.set(this, 'selection', content.objectAt(selectedIndex));
	}
});
ContainerWiz.pivot = function (source) {
	var data = {};
	source.forEach(function (item) {
		var name = item.name.replace(/\[\d+\]$/, '');
		if (!data[name]) {
			data[name] = [];
		}
		var index = (item.name.match(/\[(\d+)\]$/)||[])[1] || data[name].length;
		data[name][index] = item.value;
	});
	return data;
};
ContainerWiz.XmlMoConfView = Ember.ContainerView.extend(RSuite.view.Dialog, {
	index: 0,
	classNames: ['xml-mo-conf-view'],
	title: function () {
		return "Create Product Wizard";
	}.property('subPageIndex'),
	icon: 'createFromTemplate',
	sectionType: function (name, value) {
		if (arguments.length === 2) {
			this.set('index', this.get('sectionTypes').indexOf(value));
		}
		return this.get('sectionTypes').objectAt(this.get('index'));
	}.property('sectionTypes.@each', 'index'),
	sectionTypes: null,
	values: function () { return [Ember.Object.create({ index: 0 })]; }.property(),
	validationRules: {
		"If title is provided, a template must be selected": function (data) {
			var ok = true;
			data.templateId.forEach(function (templateId, index) {
				if (data.title[index]) {
					ok = ok && !!templateId;
				}
			});
			return ok;
		}
	},
	problems: null,
	validate: function (context) {
		if (!context) { context = this.prepareResponse(); }
		//Pivot data set
		var data = ContainerWiz.pivot(context.data);
		var ok = true,
			rules = this.validationRules,
			problems = [];
		Object.keys(rules).forEach(function (ruleName) {
			var rule = rules[ruleName];
			ok = ok && rule.call(this, data);
			if (!ok) {
				problems.push(ruleName);
			}
		}, this);
		this.set('problems', problems);
		Ember.run.schedule('timers', this, 'reposition');
		return ok;
	},
	prepareResponse: function (context) {
		context = context || {};
		Ember.set(context, 'data', [
			{ name: "passThruTest", value: this.get('passThruTest') },
			{ name: "confAlias", value: this.get('sectionTypes.confAlias') },
			{ name: "containerWizard", value: this.get('containerWizard') },
			{ name: "nextPageIdx", value: this.get('nextPageIdx') },
			{ name: "sectionTypeIdx", value: this.get('index') },
			{ name: "subPageIdx", value: this.get('subPageIndex') },
			{ name: "nextSubPageIdx", value: this.get('subPageIndex') + Ember.get(context, 'changePage') }
		].concat(this.get('values').reduce(function (data, value, index) {
			return data.concat([
				{ name: 'templateId[' + index + ']', value: value.templateId },
				{ name: 'title[' + index + ']', value: value.title },
				//{ name: 'subTitle[' + index + ']', value: value.subTitle }
			]);
		}, [])));
		return context;
	},
	resolve: function (context) {
		context = this.prepareResponse(context);
		if (!this.validate(context)) {
			return this;
		}
		var handle = this.get('deferredHandle');
		handle.resolveWith(this, [context]);
		return this;
	},
	reject: function () {
		var handle = this.get('deferredHandle');
		handle.rejectWith(this, arguments);
		return this;
	},
	adjustValueIndices: function () {
		this.get('values').forEach(function (value, index) {
			if (value.index !== index) {
				value.set('index', index);
			}
		});
	},
	init: function () {
		this._super();
		this.set('problems', []);
	},
	childViews: [
		RSuite.View.extend({
			classNames: ['config-container'],
			submit: function() { return false; },
			subPageIndex: 1,
			value: null,
			templateName: RSuite.url(ContainerWiz.pluginId, "ContainerWiz/XmlMoConfView.hbr"),
			sectionTypesBinding: "parentView.sectionTypes",
			sectionTypeBinding: "parentView.sectionType",
			xmlTemplatesBinding: "parentView.xmlTemplates",
			AddButton: RSuite.view.Icon.extend({
				model: 'add',
				isVisible: function () {
					return this.get('parentView.sectionType.mayRepeat');
				}.property('parentView.sectionType.mayRepeat'),
				click: function () {
					var v = this.get('parentView'),
						values = v.get('instances'),
						last = values[values.length - 1],
						newEntry = Ember.Object.create({
							index: values.length,
							templateId: null
						});
					values.pushObject(newEntry);
					//newEntry.set('templateId', last.get('templateId'));
					Ember.run.schedule('timers', this, function () {
						newEntry.set('templateId', last.get('templateId'));
					});
					v.get('parentView').adjustValueIndices();
					Ember.run.schedule('render', this.get('parentView.parentView'), 'validate');
				}
			}),
			SectionTypeSelector: Ember.Select.extend({
				name: "sectionType",
				contentBinding: "parentView.sectionTypes",
				valueBinding: "parentView.sectionType",
				optionLabelPath: "content.name",
				optionValuePath: "content",
				change: function () { Ember.run.schedule('render', this.get('parentView.parentView'), 'validate'); }
			}),
			TemplateSelector: Ember.Select.extend({
				// TODO: Only require a value for the first displayed Template select when the selected section type is required (1 of 4 values returned by web service).
				name: 'template',
				contentBinding: "parentView.sectionType.xmlTemplates",
				instanceBinding: "templateData.keywords.instance",
				valueBinding: "instance.templateId",
				optionLabelPath: "content.name",
				optionValuePath: "content.moid",
				prompt: function () {
					var index = this.get('instance.index');
					return null;
				}.property('instance.index'),
				contentLengthChanged: Ember.observer(function () {
					Ember.run.schedule('timers', this, function () {
						if (this.get('value') !== this.$().val()) {
							this.set('value', this.get('content.0.moid'));
						}
					});
				}, 'content.length'),
				change: function () { Ember.run.schedule('render', this.get('parentView.parentView'), 'validate'); }
			}),
			TitleField: Ember.TextField.extend({
				name: 'title',
				valueBinding: "templateData.keywords.instance.title",
				change: function () { Ember.run.schedule('render', this.get('parentView.parentView'), 'validate'); }
			}),
//			SubtitleField: Ember.TextField.extend({
//				name: 'remove',
//				valueBinding: "templateData.keywords.instance.subTitle",
//				change: function () { Ember.run.schedule('render', this.get('parentView.parentView'), 'validate'); }
//			}),
			RemoveButton: RSuite.view.Icon.extend({
				model: 'delete',
				isVisible: function () {
					var sectionType = this.get('parentView.sectionType'),
						insts = this.get('parentView.instances.length');
					return sectionType.get('mayRepeat') && (insts > 1 || !sectionType.get('required'))
				}.property('parentView.sectionType.mayRepeat', 'parentView.sectionType.required', 'parentView.instances.length'),
				click: function () {
					var insts = this.get('parentView.instances'),
						i = this.get('templateData.keywords.instance'),
						view = this.get('parentView.parentView');
					Ember.run.schedule('render', view, 'validate');
					insts.removeObject(i);
					view.adjustValueIndices();

				}
			}),
			instancesBinding: 'parentView.values',
			didInsertElement: function () {
				Ember.run.schedule('render', this.get('parentView'), 'reposition');
			}
		}),
		Ember.ContainerView.extend({
			classNames: [ 'ui-buttonset', 'ui-dialog-buttonset' ],
			childViews: [
			    /* disable Back button until it functions fully.
				Ember.ContainerView.extend({
					tagName: 'button',
					isVisible: function () {
						var idx = this.get('parentView.parentView.subPageIndex');
						return idx >= 1;
					}.property('parentView.parentView.subPageIndex'),
					classNames: [ 'ui-clickable', 'ui-button', 'ui-button-text-icon-primary'],
					childViews: [
						RSuite.view.Icon.extend({
							model: 'moveBack',
							classNames: [ 'ui-icon', 'ui-button-icon-primary'],
							style: 'background-image: none'
						}),
						Ember.View.extend({
							tagName: 'span',
							classNames: [ 'ui-button-text' ],
							template: Ember.Handlebars.compile("Back")
						})
					],
					click: function () {
						this.get('parentView.parentView').resolve({ changePage: -1 });
					}
				}),
				*/
				Ember.ContainerView.extend({
					tagName: 'button',
					isVisible: function () {
						// No way to suss logic for this; how many pages are available again?
						return true;
					}.property(),
					classNames: [ 'ui-clickable', 'ui-button', 'ui-button-text-icon-secondary'],
					childViews: [
						Ember.View.extend({
							tagName: 'span',
							classNames: [ 'ui-button-text' ],
							template: Ember.Handlebars.compile("Next")
						}),
						RSuite.view.Icon.extend({
							model: 'moveForward',
							classNames: [ 'ui-icon', 'ui-button-icon-secondary'],
							style: 'background-image: none'
						})
					],
					click: function () {
						this.get('parentView.parentView').resolve({ changePage: +1 });
					}
				}),
				Ember.ContainerView.extend({
					tagName: 'button',
					classNames: [ 'ui-clickable', 'ui-button', 'ui-button-cancel', 'ui-button-text-only'],
					childViews: [
						Ember.View.extend({
							tagName: 'span',
							classNames: 'ui-button-text',
							template: Ember.Handlebars.compile("Cancel")
						})
					]
				})
			]
		})
	]
});
RSuite.Action({
	id: ContainerWiz.pluginId + ":wizardPage",
	invoke: function (context) {
		var bad = Object.keys(Ember.Object.proto()).reduce(function (bad, argName) {
			bad[argName] = true;
			return bad;
		}, {});
		var good = ContainerWiz.pivot(Object.keys(context).reduce(function (good, argName) {
			if (!bad[argName]) {
				good.push({ name: argName, value: Ember.get(context, argName) });
			}
			return good;
		}, []));

		var result = new $.Deferred(),
			values = (good.templateId || []).reduce(function (arr, id, index) {
				arr.push(Ember.Object.create({
					templateId: id,
					title: good.title[index] || '',
					//subTitle: good.subTitle[index] || '',
					index: index
				}));
				return arr;
			}, []),
			sectionConfig = Ember.get(context, 'confAlias'),
			nextPageIdx = Ember.get(context, 'nextPageIdx'),
			submitTo = Ember.get(context, 'apiName')
			subPage = Ember.get(context, 'nextSubPageIdx'),
			passThruTest = Ember.get(context, 'passThruTest'),
			containerWizard = Ember.get(context, 'containerWizard'),
			model = ContainerWiz.SectionType.getCached(sectionConfig + ":" + subPage);
		if (!values.length) {
            values = [Ember.Object.create({index: 0})];
        }
        model.done(function () {
			var dlg = ContainerWiz.XmlMoConfView.extend({
				sectionTypes: model,
				nextPageIdx: nextPageIdx,
				subPageIndex: subPage,
				passThruTest: passThruTest,
				containerWizard: containerWizard,
				values: values
			}).create();
			dlg.done(function (newContext) {
				RSuite.services({
					service: 'api/' + submitTo,
					data: newContext.data
				});
			});
			dlg.always(dlg.dialogClose.bind(dlg));
			dlg.dialogShow();
			dlg.done(result.done.bind(result));
		}).fail(result.reject.bind(result));
		return result;
	}
});
